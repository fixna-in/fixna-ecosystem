package in.fixna.invoiceassistant.invoice;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import in.fixna.invoiceassistant.document.FieldSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One extracted field with provenance and confidence (standalone V3, ADR-011/012). */
@Entity
@Table(name = "invoice_document_fields")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceDocumentField {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "extraction_id", nullable = false)
    private UUID extractionId;

    @Column(name = "field_key", nullable = false, length = 60)
    private String fieldKey;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    @Column(name = "money_amount", precision = 14, scale = 2)
    private BigDecimal moneyAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FieldSource source;

    @Column
    private Integer page;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> bbox;

    @Column(name = "raw_snippet", columnDefinition = "TEXT")
    private String rawSnippet;

    @Column(name = "extractor_version", nullable = false, length = 40)
    private String extractorVersion;

    @Column(name = "confirmed_by_user_id")
    private UUID confirmedByUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
