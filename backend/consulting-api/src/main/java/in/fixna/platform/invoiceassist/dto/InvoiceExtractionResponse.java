package in.fixna.platform.invoiceassist.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceExtractionResponse(
        UUID extractionId,
        UUID documentId,
        String status,
        int methodNativePages,
        int methodOcrPages,
        String extractorVersion,
        OffsetDateTime createdAt,
        List<InvoiceFieldResponse> fields) {

    public static InvoiceExtractionResponse from(
            in.fixna.platform.invoiceassist.InvoiceDocumentExtraction extraction,
            List<InvoiceFieldResponse> fields) {
        return new InvoiceExtractionResponse(
                extraction.getId(),
                extraction.getDocumentId(),
                extraction.getStatus().name(),
                extraction.getMethodNativePages(),
                extraction.getMethodOcrPages(),
                extraction.getExtractorVersion(),
                extraction.getCreatedAt(),
                fields);
    }
}