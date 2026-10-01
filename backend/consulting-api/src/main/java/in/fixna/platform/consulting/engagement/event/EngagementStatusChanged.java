package in.fixna.platform.consulting.engagement.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.engagement.EngagementStatus;
import in.fixna.platform.event.DomainEvent;

public record EngagementStatusChanged(
        UUID engagementId,
        UUID tenantId,
        EngagementStatus fromStatus,
        EngagementStatus toStatus,
        OffsetDateTime occurredAt)
        implements DomainEvent {

    public EngagementStatusChanged(
            UUID engagementId, UUID tenantId, EngagementStatus fromStatus, EngagementStatus toStatus) {
        this(engagementId, tenantId, fromStatus, toStatus, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "engagement.status_changed";
    }

    @Override
    public UUID tenantId() {
        return tenantId;
    }

    @Override
    public OffsetDateTime occurredAt() {
        return occurredAt;
    }
}
