package in.fixna.invoiceassistant.invoice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import in.fixna.invoiceassistant.invoice.InvoiceDocumentExtraction;

public record InvoiceExtractionResponse(
        UUID extractionId,
        UUID documentId,
        String status,
        int methodNativePages,
        int methodOcrPages,
        String extractorVersion,
        UUID postedInvoiceRecordId,
        OffsetDateTime createdAt,
        List<InvoiceFieldResponse> fields) {

    public static InvoiceExtractionResponse from(
            InvoiceDocumentExtraction extraction,
            List<InvoiceFieldResponse> fields) {
        return new InvoiceExtractionResponse(
                extraction.getId(),
                extraction.getDocumentId(),
                extraction.getStatus().name(),
                extraction.getMethodNativePages(),
                extraction.getMethodOcrPages(),
                extraction.getExtractorVersion(),
                extraction.getPostedInvoiceRecordId(),
                extraction.getCreatedAt(),
                fields);
    }
}
