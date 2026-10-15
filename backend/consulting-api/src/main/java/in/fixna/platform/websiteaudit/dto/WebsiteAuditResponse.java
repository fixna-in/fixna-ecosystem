package in.fixna.platform.websiteaudit.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import in.fixna.platform.websiteaudit.WebsiteAudit;
import in.fixna.platform.websiteaudit.WebsiteAuditCheck;

import in.fixna.platform.websiteaudit.WebsiteAudit;
import in.fixna.platform.websiteaudit.WebsiteAuditCheck;

public record WebsiteAuditResponse(
        UUID id,
        String inputUrl,
        UUID profileId,
        String status,
        int overallScore,
        String summary,
        OffsetDateTime createdAt,
        List<WebsiteAuditCheckResponse> checks) {

    public static WebsiteAuditResponse from(WebsiteAudit audit, List<WebsiteAuditCheck> checks) {
        return new WebsiteAuditResponse(
                audit.getId(),
                audit.getInputUrl(),
                audit.getProfileId(),
                audit.getStatus().name(),
                audit.getOverallScore(),
                audit.getSummary(),
                audit.getCreatedAt(),
                checks == null ? List.of() : checks.stream().map(WebsiteAuditCheckResponse::from).toList());
    }
}
