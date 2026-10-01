package in.fixna.platform.consulting.project.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.event.DomainEvent;

public record ProjectCreated(UUID projectId, UUID tenantId, OffsetDateTime occurredAt) implements DomainEvent {

    public ProjectCreated(UUID projectId, UUID tenantId) {
        this(projectId, tenantId, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "project.created";
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
