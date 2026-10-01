package in.fixna.platform.consulting.project.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.project.ProjectStatus;
import in.fixna.platform.event.DomainEvent;

public record ProjectStatusChanged(
        UUID projectId,
        UUID tenantId,
        ProjectStatus fromStatus,
        ProjectStatus toStatus,
        OffsetDateTime occurredAt)
        implements DomainEvent {

    public ProjectStatusChanged(UUID projectId, UUID tenantId, ProjectStatus fromStatus, ProjectStatus toStatus) {
        this(projectId, tenantId, fromStatus, toStatus, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "project.status_changed";
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
