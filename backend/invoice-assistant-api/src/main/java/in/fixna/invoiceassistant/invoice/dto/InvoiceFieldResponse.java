package in.fixna.invoiceassistant.invoice.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record InvoiceFieldResponse(
        UUID id,
        String fieldKey,
        String fieldValue,
        BigDecimal moneyAmount,
        String currency,
        BigDecimal confidence,
        String source,
        Integer page,
        Map<String, Object> bbox,
        String rawSnippet) {

    public static InvoiceFieldResponse from(in.fixna.invoiceassistant.invoice.InvoiceDocumentField field) {
        return new InvoiceFieldResponse(
                field.getId(),
                field.getFieldKey(),
                field.getFieldValue(),
                field.getMoneyAmount(),
                field.getCurrency(),
                field.getConfidence(),
                field.getSource().name(),
                field.getPage(),
                field.getBbox(),
                field.getRawSnippet());
    }
}
