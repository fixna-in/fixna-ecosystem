package in.fixna.platform.invoiceassist.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import in.fixna.platform.invoiceassist.InvoiceSuggestion;

public record InvoiceSuggestionResponse(
        UUID id,
        UUID invoiceId,
        String suggestionType,
        String status,
        Map<String, String> payload,
        OffsetDateTime createdAt) {

    public static InvoiceSuggestionResponse from(InvoiceSuggestion suggestion) {
        return new InvoiceSuggestionResponse(
                suggestion.getId(),
                suggestion.getInvoiceId(),
                suggestion.getSuggestionType().name(),
                suggestion.getStatus().name(),
                suggestion.getPayload(),
                suggestion.getCreatedAt());
    }
}
