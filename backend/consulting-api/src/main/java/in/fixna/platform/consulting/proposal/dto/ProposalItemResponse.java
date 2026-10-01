package in.fixna.platform.consulting.proposal.dto;

import java.math.BigDecimal;
import java.util.UUID;

import in.fixna.platform.consulting.proposal.ProposalItem;

public record ProposalItemResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        int sortOrder) {

    public static ProposalItemResponse from(ProposalItem item) {
        return new ProposalItemResponse(
                item.getId(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal(),
                item.getSortOrder());
    }
}
