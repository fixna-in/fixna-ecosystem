package in.fixna.invoiceassistant.invoice;

import java.time.LocalDate;
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

/**
 * Standalone invoice record owned by Invoice Assistant (V2). No consulting
 * FKs — this product owns its own invoice data.
 */
@Entity
@Table(name = "invoice_records")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vendor_name", columnDefinition = "TEXT")
    private String vendorName;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 3)
    private String currency;

    @Column(precision = 14, scale = 2)
    private java.math.BigDecimal subtotal;

    @Column(name = "tax_total", precision = 14, scale = 2)
    private java.math.BigDecimal taxTotal;

    @Column(name = "grand_total", precision = 14, scale = 2)
    private java.math.BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceRecordStatus status = InvoiceRecordStatus.DRAFT;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

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
