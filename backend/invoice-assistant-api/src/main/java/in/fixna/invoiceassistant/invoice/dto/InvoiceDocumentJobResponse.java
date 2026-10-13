package in.fixna.invoiceassistant.invoice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import in.fixna.invoiceassistant.invoice.InvoiceDocumentJobStatus;

/** Response for async invoice document extraction job polling. */
public record InvoiceDocumentJobResponse(
        @JsonProperty("jobId") UUID jobId,
        @JsonProperty("documentId") UUID documentId,
        @JsonProperty("status") InvoiceDocumentJobStatus status,
        @JsonProperty("errorCode") String errorCode,
        @JsonProperty("errorMessage") String errorMessage,
        @JsonProperty("createdAt") OffsetDateTime createdAt) {}
