package in.fixna.platform.consulting.proposal.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.event.DomainEvent;

public record ProposalApproved(UUID proposalId, UUID tenantId, OffsetDateTime occurredAt) implements DomainEvent {

    public ProposalApproved(UUID proposalId, UUID tenantId) {
        this(proposalId, tenantId, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "proposal.approved";
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
