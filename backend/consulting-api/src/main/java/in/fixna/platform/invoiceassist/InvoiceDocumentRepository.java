package in.fixna.platform.invoiceassist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocument, UUID> {

    Optional<InvoiceDocument> findByIdAndTenantId(UUID id, UUID tenantId);

    List<InvoiceDocument> findByInvoiceIdAndTenantIdOrderByCreatedAtDesc(UUID invoiceId, UUID tenantId);

    boolean existsByInvoiceIdAndTenantIdAndSha256(UUID invoiceId, UUID tenantId, String sha256);

    /** Retention scan: documents past their purge date that still hold bytes. */
    List<InvoiceDocument> findByPurgeAfterBeforeAndStatusNot(java.time.OffsetDateTime cutoff,
            InvoiceDocumentStatus status);
}