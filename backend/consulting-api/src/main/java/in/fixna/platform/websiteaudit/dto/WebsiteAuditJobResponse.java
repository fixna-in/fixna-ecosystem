package in.fixna.platform.websiteaudit.dto;

import java.util.UUID;

/** Job resource returned by submit/poll/cancel (ADR-009). */
public record WebsiteAuditJobResponse(
        UUID jobId,
        String status,
        int attempt,
        int maxAttempts,
        Progress progress,
        String failureCode,
        UUID auditId,
        String linksSelf,
        String linksAudit) {

    public record Progress(int pagesFetched, int budgetPages, int budgetSeconds) {}

    public static WebsiteAuditJobResponse pending(
            UUID jobId, String status, int attempt, int maxAttempts,
            int pagesFetched, int budgetPages, int budgetSeconds,
            String failureCode, UUID auditId) {
        return new WebsiteAuditJobResponse(
                jobId, status, attempt, maxAttempts,
                new Progress(pagesFetched, budgetPages, budgetSeconds),
                failureCode, auditId,
                "/api/v1/website-audits/jobs/" + jobId,
                auditId == null ? null : "/api/v1/website-audits/" + auditId);
    }
}