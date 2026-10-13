package in.fixna.websiteaudit.job;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.websiteaudit.WebsiteAudit;
import in.fixna.websiteaudit.WebsiteAuditCheck;
import in.fixna.websiteaudit.WebsiteAuditCheckRepository;
import in.fixna.websiteaudit.WebsiteAuditJob;
import in.fixna.websiteaudit.WebsiteAuditJobRepository;
import in.fixna.websiteaudit.WebsiteAuditPage;
import in.fixna.websiteaudit.WebsiteAuditPageRepository;
import in.fixna.websiteaudit.WebsiteAuditRepository;
import in.fixna.websiteaudit.WebsiteAuditStatus;
import in.fixna.websiteaudit.WebsiteAuditJobStatus;
import in.fixna.websiteaudit.check.WebsiteAuditor.AuditOutcome;
import in.fixna.websiteaudit.check.WebsiteAuditor.CheckResult;
import in.fixna.websiteaudit.check.WebsiteCrawler.CrawlBounds;
import in.fixna.websiteaudit.check.WebsiteCrawler.CrawlResult;
import in.fixna.websiteaudit.fetcher.WebsiteFetcher.FetchedPage;

/**
 * Transactional boundary for job state changes and result persistence (WB-03/04).
 * The worker stays outside long-running crawls; each call here is a short tx.
 */
@Service
public class WebsiteAuditPersistence {

    private final WebsiteAuditJobRepository jobs;
    private final WebsiteAuditPageRepository pages;
    private final WebsiteAuditRepository audits;
    private final WebsiteAuditCheckRepository checks;

    public WebsiteAuditPersistence(
            WebsiteAuditJobRepository jobs,
            WebsiteAuditPageRepository pages,
            WebsiteAuditRepository audits,
            WebsiteAuditCheckRepository checks) {
        this.jobs = jobs;
        this.pages = pages;
        this.audits = audits;
        this.checks = checks;
    }

    @Transactional
    public int claim(UUID jobId, String workerId, OffsetDateTime leaseUntil) {
        return jobs.claim(jobId, workerId, leaseUntil, OffsetDateTime.now(),
                WebsiteAuditJobStatus.QUEUED, WebsiteAuditJobStatus.RUNNING);
    }

    @Transactional
    public int heartbeat(UUID jobId, String workerId) {
        return jobs.heartbeat(jobId, workerId, OffsetDateTime.now().plusSeconds(90),
                OffsetDateTime.now(), WebsiteAuditJobStatus.RUNNING);
    }

    @Transactional
    public int finish(UUID jobId, String workerId, WebsiteAuditJobStatus target, String failureCode) {
        return jobs.finishIfLocked(jobId, workerId, target, failureCode,
                OffsetDateTime.now(), WebsiteAuditJobStatus.RUNNING);
    }

    @Transactional
    public int requeueExpired() {
        return jobs.requeueExpired(OffsetDateTime.now(),
                WebsiteAuditJobStatus.QUEUED, WebsiteAuditJobStatus.RUNNING);
    }

    @Transactional
    public int cancel(WebsiteAuditJob job) {
        int updated = jobs.cancelIfActive(job.getId(), OffsetDateTime.now(),
                WebsiteAuditJobStatus.CANCELLED,
                List.of(WebsiteAuditJobStatus.QUEUED, WebsiteAuditJobStatus.RUNNING));
        if (updated == 1 && job.getAuditId() != null) {
            audits.findById(job.getAuditId()).ifPresent(audit -> {
                audit.setStatus(WebsiteAuditStatus.FAILED);
                audit.setSummary("Audit cancelled before completion.");
                audits.save(audit);
            });
            pages.deleteByJobId(job.getId());
        }
        return updated;
    }

    /** Marks a job failed outside the worker lock (attempt cap exhausted). */
    @Transactional
    public void failExhausted(WebsiteAuditJob job) {
        job.setStatus(WebsiteAuditJobStatus.FAILED);
        job.setFailureCode("RECOVERY_EXHAUSTED");
        job.setLockedBy(null);
        job.setLeaseExpiresAt(null);
        jobs.save(job);
    }

    @Transactional
    public Optional<WebsiteAuditJob> findNextQueued() {
        return jobs.findFirstByStatusOrderByCreatedAtDesc(WebsiteAuditJobStatus.QUEUED);
    }

    @Transactional
    public Optional<WebsiteAuditJob> loadJob(UUID jobId) {
        return jobs.findById(jobId);
    }

    /**
     * Persists a completed crawl: audit summary/score, checks, and per-page
     * rows. Marks the audit COMPLETED when the seed was fetched, else FAILED.
     * Runs as one short transaction after the (long, untransacted) crawl.
     */
    @Transactional
    public void persistResult(WebsiteAuditJob job, CrawlResult crawl, AuditOutcome outcome) {
        WebsiteAudit audit = audits.findById(job.getAuditId()).orElse(null);
        if (audit == null) {
            return;
        }
        boolean succeeded = crawl.seed() != null;
        audit.setStatus(succeeded ? WebsiteAuditStatus.COMPLETED : WebsiteAuditStatus.FAILED);
        audit.setOverallScore(outcome.overallScore());
        audit.setSummary(orEmpty(outcome.summary()));
        audits.save(audit);

        for (CheckResult check : outcome.checks()) {
            WebsiteAuditCheck entity = new WebsiteAuditCheck();
            entity.setId(UUID.randomUUID());
            entity.setTenantId(job.getTenantId());
            entity.setAuditId(audit.getId());
            entity.setCheckCode(check.code());
            entity.setSeverity(check.severity());
            entity.setScore(check.score());
            entity.setEvidence(check.evidence());
            entity.setFixHint(check.fixHint());
            checks.save(entity);
        }

        int index = 0;
        for (FetchedPage page : crawl.pages()) {
            String hash = sha256Hex(normalized(page.requestedUri()));
            if (pages.existsByAuditIdAndUrlHash(audit.getId(), hash)) {
                index++;
                continue;
            }
            WebsiteAuditPage row = new WebsiteAuditPage();
            row.setId(UUID.randomUUID());
            row.setTenantId(job.getTenantId());
            row.setAuditId(audit.getId());
            row.setJobId(job.getId());
            row.setUrl(page.requestedUri().toString());
            row.setUrlHash(hash);
            row.setDepth(index == 0 ? 0 : Math.min(index, 2));
            row.setHttpStatus(page.statusCode());
            String contentType = page.contentType();
            row.setContentType(contentType == null ? null
                    : contentType.substring(0, Math.min(100, contentType.length())));
            row.setSizeBytes(page.body() == null ? 0 : page.body().length());
            row.setFetchMs((int) page.durationMs());
            pages.save(row);
            index++;
        }
    }

    static String normalized(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + host + path;
    }

    static String sha256Hex(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String orEmpty(String value) {
        return value == null || value.isBlank() ? "review recommended" : value;
    }
}