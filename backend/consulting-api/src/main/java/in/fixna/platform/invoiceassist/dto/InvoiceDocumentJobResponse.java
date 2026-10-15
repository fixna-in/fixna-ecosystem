package in.fixna.platform.invoiceassist.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import in.fixna.platform.invoiceassist.InvoiceDocumentJobStatus;

/**
 * Response for async invoice document extraction job.
 */
public record InvoiceDocumentJobResponse(
        @JsonProperty("jobId") UUID jobId,
        @JsonProperty("status") InvoiceDocumentJobStatus status,
        @JsonProperty("createdAt") OffsetDateTime createdAt) {}