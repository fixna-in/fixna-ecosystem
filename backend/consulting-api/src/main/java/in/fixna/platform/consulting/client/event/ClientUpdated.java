package in.fixna.platform.consulting.client.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.event.DomainEvent;

public record ClientUpdated(UUID clientId, UUID tenantId, OffsetDateTime occurredAt) implements DomainEvent {

    public ClientUpdated(UUID clientId, UUID tenantId) {
        this(clientId, tenantId, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "client.updated";
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
