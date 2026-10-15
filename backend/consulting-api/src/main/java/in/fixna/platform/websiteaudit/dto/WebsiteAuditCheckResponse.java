package in.fixna.platform.websiteaudit.dto;

import java.util.Map;
import java.util.UUID;

import in.fixna.platform.websiteaudit.WebsiteAuditCheck;

import in.fixna.platform.websiteaudit.WebsiteAuditCheck;

public record WebsiteAuditCheckResponse(
        UUID id, String checkCode, String severity, int score, String fixHint, Map<String, String> evidence) {

    public static WebsiteAuditCheckResponse from(WebsiteAuditCheck check) {
        return new WebsiteAuditCheckResponse(
                check.getId(),
                check.getCheckCode(),
                check.getSeverity().name(),
                check.getScore(),
                check.getFixHint(),
                check.getEvidence());
    }
}
