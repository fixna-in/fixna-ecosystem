package in.fixna.platform.invoiceassist.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import in.fixna.platform.invoiceassist.InvoiceExtractionStatus;

/**
 * Summary view of an invoice extraction for search results.
 */
public record InvoiceExtractionSummary(
        @JsonProperty("id") UUID id,
        @JsonProperty("documentId") UUID documentId,
        @JsonProperty("invoiceId") UUID invoiceId,
        @JsonProperty("status") InvoiceExtractionStatus status,
        @JsonProperty("createdAt") OffsetDateTime createdAt,
        @JsonProperty("fieldCount") int fieldCount,
        @JsonProperty("totalAmount") BigDecimal totalAmount) {}