package in.fixna.platform.websiteaudit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.profile.ConsultingProfile;
import in.fixna.platform.consulting.profile.ConsultingProfileRepository;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;
import in.fixna.platform.websiteaudit.dto.WebsiteAuditJobResponse;
import in.fixna.platform.websiteaudit.dto.WebsiteAuditRequest;

/**
 * Request-facing job orchestration for bounded website audits (ADR-009).
 * Submitting returns a job (202 model); a background worker claims and runs
 * it. Polling is tenant-scoped and never mutates job state.
 */
@Service
public class WebsiteAuditJobService {

    static final int MAX_ATTEMPTS = 3;
    static final int BUDGET_PAGES = 25;
    static final int BUDGET_SECONDS = 60;

    private final WebsiteAuditJobRepository jobs;
    private final WebsiteAuditRepository audits;
    private final WebsiteAuditPageRepository pages;
    private final WebsiteAuditPersistence persistence;
    private final ConsultingProfileRepository profiles;
    private final AuditPublisher audit;

    public WebsiteAuditJobService(
            WebsiteAuditJobRepository jobs,
            WebsiteAuditRepository audits,
            WebsiteAuditPageRepository pages,
            WebsiteAuditPersistence persistence,
            ConsultingProfileRepository profiles,
            AuditPublisher audit) {
        this.jobs = jobs;
        this.audits = audits;
        this.pages = pages;
        this.persistence = persistence;
        this.profiles = profiles;
        this.audit = audit;
    }

    /** Creates a RUNNING audit + QUEUED job and returns the job resource. */
    @Transactional
    public WebsiteAuditJobResponse submit(WebsiteAuditRequest request) {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        jobs.findFirstByTenantIdAndStatus(tenantId, WebsiteAuditJobStatus.RUNNING)
                .ifPresent(running -> {
                    throw new FixnaException(
                            "CRAWL_IN_PROGRESS",
                            HttpStatus.CONFLICT,
                            "A website audit crawl is already running for this workspace");
                });
        URI target = parseTarget(request.url());
        String hash = WebsiteAuditPersistence.sha256Hex(WebsiteAuditPersistence.normalized(target));

        WebsiteAudit auditEntity = new WebsiteAudit();
        auditEntity.setId(UUID.randomUUID());
        auditEntity.setTenantId(tenantId);
        auditEntity.setProfileId(matchProfile(tenantId, target));
        auditEntity.setInputUrl(request.url().trim());
        auditEntity.setNormalizedUrlHash(hash);
        auditEntity.setCreatedByUserId(userId);
        auditEntity.setStatus(WebsiteAuditStatus.RUNNING);
        auditEntity.setOverallScore(0);
        audits.save(auditEntity);

        WebsiteAuditJob job = new WebsiteAuditJob();
        job.setId(UUID.randomUUID());
        job.setTenantId(tenantId);
        job.setAuditId(auditEntity.getId());
        job.setRequestedUrl(request.url().trim());
        job.setCrawlEnabled(request.crawlEnabled());
        job.setStatus(WebsiteAuditJobStatus.QUEUED);
        job.setCreatedByUserId(userId);
        jobs.save(job);

        audit.publish(new AuditEvent(
                "website.audit.queued",
                tenantId,
                userId,
                "website_audit_job",
                job.getId().toString(),
                Map.of("url", auditEntity.getInputUrl(), "crawl", String.valueOf(job.isCrawlEnabled())),
                null));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public WebsiteAuditJobResponse poll(UUID jobId) {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_VIEW);
        return toResponse(requireJob(jobId));
    }

    @Transactional
    public WebsiteAuditJobResponse cancel(UUID jobId) {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_MANAGE);
        WebsiteAuditJob job = requireJob(jobId);
        if (job.getStatus().isTerminal()) {
            throw new FixnaException(
                    "JOB_NOT_CANCELLABLE",
                    HttpStatus.CONFLICT,
                    "Job is already in a terminal state");
        }
        persistence.cancel(job);
        audit.publish(new AuditEvent(
                "website.audit.cancelled",
                job.getTenantId(),
                TenantContext.requireUserId(),
                "website_audit_job",
                jobId.toString(),
                Map.of(),
                null));
        return toResponse(jobs.findById(jobId).orElse(job));
    }

    @Transactional(readOnly = true)
    public List<WebsiteAuditJobResponse> list() {
        TenantContext.requirePermission(Permission.WEBSITE_AUDIT_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        return jobs.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    private WebsiteAuditJob requireJob(UUID jobId) {
        UUID tenantId = TenantContext.requireTenantId();
        return jobs.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Website audit job not found"));
    }

    private WebsiteAuditJobResponse toResponse(WebsiteAuditJob job) {
        int pagesFetched = job.getAuditId() == null
                ? 0
                : (int) pages.countByJobId(job.getId());
        return WebsiteAuditJobResponse.pending(
                job.getId(),
                job.getStatus().name(),
                job.getAttempt(),
                MAX_ATTEMPTS,
                pagesFetched,
                BUDGET_PAGES,
                BUDGET_SECONDS,
                job.getFailureCode(),
                job.getAuditId());
    }

    private static URI parseTarget(String raw) {
        try {
            URI uri = new URI(raw.trim());
            String scheme = uri.getScheme();
            boolean http = scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
            if (!http || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new FixnaException("INVALID_URL", HttpStatus.BAD_REQUEST, "URL must be a valid http(s) URL");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new FixnaException("INVALID_URL", HttpStatus.BAD_REQUEST, "URL must be a valid http(s) URL");
        }
    }

    private UUID matchProfile(UUID tenantId, URI target) {
        return profiles.findByTenantId(tenantId)
                .filter(profile -> sameHost(profile.getWebsiteUrl(), target.getHost()))
                .map(ConsultingProfile::getId)
                .orElse(null);
    }

    private static boolean sameHost(String profileUrl, String targetHost) {
        if (profileUrl == null || profileUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(profileUrl.trim());
            return uri.getHost() != null && uri.getHost().equalsIgnoreCase(targetHost);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}