package in.fixna.invoiceassistant.invoice;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks monthly quotas for invoice document uploads and extractions per tenant.
 */
@Entity
@Table(name = "tenant_quotas")
@Getter
@Setter
@NoArgsConstructor
public class TenantQuota {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "uploads_this_month", nullable = false)
    private int uploadsThisMonth = 0;

    @Column(name = "extractions_this_month", nullable = false)
    private int extractionsThisMonth = 0;

    @Column(name = "reset_at", nullable = false)
    private OffsetDateTime resetAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public TenantQuota(UUID tenantId) {
        this.tenantId = tenantId;
        this.resetAt = OffsetDateTime.now().plusMonths(1);
        this.updatedAt = OffsetDateTime.now();
    }
}
