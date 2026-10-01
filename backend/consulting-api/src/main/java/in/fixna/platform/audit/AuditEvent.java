package in.fixna.platform.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        String action,
        UUID tenantId,
        UUID actorUserId,
        String entityType,
        String entityId,
        Map<String, String> details,
        OffsetDateTime occurredAt) {

    public AuditEvent {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Audit action is required");
        }
        details = details == null ? Map.of() : Map.copyOf(details);
        occurredAt = occurredAt == null ? OffsetDateTime.now() : occurredAt;
    }
}
