package in.fixna.platform.invoiceassist.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceDocumentResponse(
        UUID documentId,
        UUID invoiceId,
        String fileName,
        long sizeBytes,
        String status,
        OffsetDateTime createdAt) {

    public static InvoiceDocumentResponse from(in.fixna.platform.invoiceassist.InvoiceDocument document) {
        return new InvoiceDocumentResponse(
                document.getId(),
                document.getInvoiceId(),
                document.getFileName(),
                document.getSizeBytes(),
                document.getStatus().name(),
                document.getCreatedAt());
    }
}