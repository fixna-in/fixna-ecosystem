package in.fixna.platform.consulting.engagement.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.event.DomainEvent;

public record EngagementCreated(UUID engagementId, UUID tenantId, OffsetDateTime occurredAt) implements DomainEvent {

    public EngagementCreated(UUID engagementId, UUID tenantId) {
        this(engagementId, tenantId, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "engagement.created";
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
