package in.fixna.platform.consulting.proposal.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.event.DomainEvent;

public record ProposalCreated(UUID proposalId, UUID tenantId, OffsetDateTime occurredAt) implements DomainEvent {

    public ProposalCreated(UUID proposalId, UUID tenantId) {
        this(proposalId, tenantId, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "proposal.created";
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
