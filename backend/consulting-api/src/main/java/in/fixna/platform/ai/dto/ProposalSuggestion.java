package in.fixna.platform.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProposalSuggestion(String title, List<ProposalLineItemSuggestion> lineItems) {

    public record ProposalLineItemSuggestion(String description, BigDecimal quantity, BigDecimal unitPrice) {}
}
