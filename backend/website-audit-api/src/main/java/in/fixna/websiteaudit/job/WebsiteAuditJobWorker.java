package in.fixna.websiteaudit.job;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import in.fixna.websiteaudit.WebsiteAuditJob;
import in.fixna.websiteaudit.WebsiteAuditJobStatus;
import in.fixna.websiteaudit.audit.AuditEvent;
import in.fixna.websiteaudit.audit.AuditPublisher;
import in.fixna.websiteaudit.check.WebsiteAuditor;
import in.fixna.websiteaudit.check.WebsiteAuditor.AuditOutcome;
import in.fixna.websiteaudit.check.WebsiteCrawler;
import in.fixna.websiteaudit.check.WebsiteCrawler.CrawlBounds;
import in.fixna.websiteaudit.check.WebsiteCrawler.CrawlResult;

/**
 * Single-node worker that drains QUEUED website audit jobs (ADR-009, WB-04).
 * The scheduler runs on a dedicated single thread so a long crawl never blocks
 * other scheduled tasks. Lease-based claiming plus startup requeue give crash
 * recovery; the attempt cap prevents poison jobs from looping forever.
 */
@Component
public class WebsiteAuditJobWorker implements SchedulingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.audit.worker");
    private static final String WORKER_ID = "website-audit-api-" + java.util.UUID.randomUUID();
    private static final Duration LEASE = Duration.ofSeconds(90);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);

    private final WebsiteAuditPersistence persistence;
    private final WebsiteCrawler crawler;
    private final WebsiteAuditor auditor;
    private final AuditPublisher audit;

    public WebsiteAuditJobWorker(
            WebsiteAuditPersistence persistence,
            WebsiteCrawler crawler,
            WebsiteAuditor auditor,
            AuditPublisher audit) {
        this.persistence = persistence;
        this.crawler = crawler;
        this.auditor = auditor;
        this.audit = audit;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "website-audit-worker");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** Crash recovery: requeue RUNNING jobs whose lease expired, on startup. */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        int requeued = persistence.requeueExpired();
        if (requeued > 0) {
            LOG.info("operation=audit_worker_recovery status=requeued count={}", requeued);
        }
    }

    @Scheduled(fixedDelayString = "${fixna.website-audit.poll-ms:3000}")
    public void drainOneJob() {
        try {
            persistence.requeueExpired();
            Optional<WebsiteAuditJob> next = persistence.findNextQueued();
            if (next.isEmpty()) {
                return;
            }
            WebsiteAuditJob job = next.get();
            int claimed = persistence.claim(
                    job.getId(), WORKER_ID, OffsetDateTime.now().plus(LEASE));
            if (claimed == 0) {
                return;
            }
            process(job.getId());
        } catch (RuntimeException e) {
            LOG.warn("operation=audit_worker_tick status=error", e);
        }
    }

    private void process(UUID jobId) {
        WebsiteAuditJob job = persistence.loadJob(jobId).orElse(null);
        if (job == null) {
            return;
        }
        if (job.getAttempt() > WebsiteAuditJobService.MAX_ATTEMPTS) {
            persistence.failExhausted(job);
            publishFailure(job, "RECOVERY_EXHAUSTED");
            LOG.warn("operation=audit_job_failed jobId={} reason=attempts_exhausted", jobId);
            return;
        }

        Heartbeat heartbeat = startHeartbeat(jobId);
        try {
            URI seed = URI.create(job.getRequestedUrl());
            CrawlResult crawl = crawler.crawl(seed, job.isCrawlEnabled(), CrawlBounds.fast());
            AuditOutcome outcome = crawl.seed() == null
                    ? WebsiteAuditor.failed("target unreachable or blocked")
                    : auditor.auditWithSite(
                            crawl.seed(), crawl.robots(), crawl.sitemap(), crawl.pages());
            persistence.persistResult(job, crawl, outcome);
            int finished = persistence.finish(jobId, WORKER_ID, WebsiteAuditJobStatus.COMPLETED, null);
            if (finished == 1) {
                publishCompleted(job, outcome);
            }
        } catch (RuntimeException e) {
            LOG.warn("operation=audit_job_run status=error jobId={}", jobId, e);
            persistence.finish(jobId, WORKER_ID, WebsiteAuditJobStatus.FAILED, "INTERNAL_ERROR");
            publishFailure(job, "INTERNAL_ERROR");
        } finally {
            heartbeat.stop();
        }
    }

    private Heartbeat startHeartbeat(UUID jobId) {
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(HEARTBEAT_INTERVAL.toMillis());
                    persistence.heartbeat(jobId, WORKER_ID);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "website-audit-heartbeat-" + jobId);
        thread.setDaemon(true);
        thread.start();
        return thread::interrupt;
    }

    private interface Heartbeat {
        void stop();
    }

    private void publishCompleted(WebsiteAuditJob job, AuditOutcome outcome) {
        audit.publish(new AuditEvent(
                "website.audit.completed",
                job.getTenantId(),
                job.getCreatedByUserId(),
                "website_audit",
                job.getAuditId() == null ? "" : job.getAuditId().toString(),
                Map.of("score", String.valueOf(outcome.overallScore())),
                null));
    }

    private void publishFailure(WebsiteAuditJob job, String code) {
        audit.publish(new AuditEvent(
                "website.audit.failed",
                job.getTenantId(),
                job.getCreatedByUserId(),
                "website_audit_job",
                job.getId().toString(),
                Map.of("failureCode", code),
                null));
    }
}