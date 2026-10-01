package in.fixna.platform.consulting.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import in.fixna.platform.consulting.invoice.Invoice;
import in.fixna.platform.consulting.invoice.InvoiceLine;
import in.fixna.platform.consulting.invoice.InvoiceStatus;

public record InvoiceResponse(
        UUID id,
        UUID clientId,
        UUID engagementId,
        UUID proposalId,
        String invoiceNumber,
        String title,
        InvoiceStatus status,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        String currency,
        LocalDate dueDate,
        OffsetDateTime sentAt,
        List<InvoiceLineResponse> lines,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static InvoiceResponse from(Invoice invoice, List<InvoiceLine> lines) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getClientId(),
                invoice.getEngagementId(),
                invoice.getProposalId(),
                invoice.getInvoiceNumber(),
                invoice.getTitle(),
                invoice.getStatus(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getAmountPaid(),
                invoice.getCurrency(),
                invoice.getDueDate(),
                invoice.getSentAt(),
                lines.stream().map(InvoiceLineResponse::from).toList(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt());
    }

    public static InvoiceResponse from(Invoice invoice) {
        return from(invoice, List.of());
    }
}
