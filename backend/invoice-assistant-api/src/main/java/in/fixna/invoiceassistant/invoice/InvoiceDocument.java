package in.fixna.invoiceassistant.invoice;

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

/** A privately uploaded invoice PDF (standalone V3, ADR-011). Never publicly reachable. */
@Entity
@Table(name = "invoice_documents")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceDocument {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Optional link to an owned invoice record; null = standalone intake. */
    @Column(name = "invoice_record_id")
    private UUID invoiceRecordId;

    @Column(name = "file_name", nullable = false, columnDefinition = "TEXT")
    private String fileName;

    @Column(name = "storage_key", columnDefinition = "TEXT")
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType = "application/pdf";

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceDocumentStatus status = InvoiceDocumentStatus.RECEIVED;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "purge_after")
    private OffsetDateTime purgeAfter;

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
