package in.fixna.invoiceassistant.invoice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.invoiceassistant.invoice.InvoiceDocument;

public record InvoiceDocumentResponse(
        UUID documentId,
        UUID invoiceRecordId,
        String fileName,
        long sizeBytes,
        String status,
        OffsetDateTime createdAt) {

    public static InvoiceDocumentResponse from(InvoiceDocument document) {
        return new InvoiceDocumentResponse(
                document.getId(),
                document.getInvoiceRecordId(),
                document.getFileName(),
                document.getSizeBytes(),
                document.getStatus().name(),
                document.getCreatedAt());
    }
}
