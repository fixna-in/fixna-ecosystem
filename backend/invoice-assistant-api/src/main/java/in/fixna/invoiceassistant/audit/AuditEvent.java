package in.fixna.invoiceassistant.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        String action,
        UUID tenantId,
        UUID userId,
        String resourceType,
        String resourceId,
        Map<String, String> details,
        OffsetDateTime occurredAt) {

    public AuditEvent(
            String action,
            UUID tenantId,
            UUID userId,
            String resourceType,
            String resourceId,
            Map<String, String> details) {
        this(action, tenantId, userId, resourceType, resourceId, details, OffsetDateTime.now());
    }
}
