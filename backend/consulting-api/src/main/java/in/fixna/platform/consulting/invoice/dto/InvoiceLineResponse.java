package in.fixna.platform.consulting.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import in.fixna.platform.consulting.invoice.InvoiceLine;

public record InvoiceLineResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        int sortOrder) {

    public static InvoiceLineResponse from(InvoiceLine line) {
        return new InvoiceLineResponse(
                line.getId(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineTotal(),
                line.getSortOrder());
    }
}
