package in.fixna.platform.invoiceassist;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One extraction run over a document (V13, ADR-011). */
@Entity
@Table(name = "invoice_document_extractions")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceDocumentExtraction {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceExtractionStatus status = InvoiceExtractionStatus.NEEDS_REVIEW;

    @Column(name = "method_native_pages", nullable = false)
    private int methodNativePages;

    @Column(name = "method_ocr_pages", nullable = false)
    private int methodOcrPages;

    @Column(name = "extractor_version", nullable = false, length = 40)
    private String extractorVersion;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "posted_invoice_id")
    private UUID postedInvoiceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}