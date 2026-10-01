package in.fixna.platform.consulting.proposal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import in.fixna.platform.consulting.proposal.Proposal;
import in.fixna.platform.consulting.proposal.ProposalItem;
import in.fixna.platform.consulting.proposal.ProposalStatus;

public record ProposalResponse(
        UUID id,
        UUID clientId,
        UUID engagementId,
        String title,
        String description,
        ProposalStatus status,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        LocalDate validUntil,
        OffsetDateTime sentAt,
        List<ProposalItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ProposalResponse from(Proposal proposal, List<ProposalItem> items) {
        return new ProposalResponse(
                proposal.getId(),
                proposal.getClientId(),
                proposal.getEngagementId(),
                proposal.getTitle(),
                proposal.getDescription(),
                proposal.getStatus(),
                proposal.getSubtotal(),
                proposal.getTaxAmount(),
                proposal.getTotalAmount(),
                proposal.getCurrency(),
                proposal.getValidUntil(),
                proposal.getSentAt(),
                items.stream().map(ProposalItemResponse::from).toList(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt());
    }

    public static ProposalResponse from(Proposal proposal) {
        return from(proposal, List.of());
    }
}
