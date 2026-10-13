package in.fixna.invoiceassistant.invoice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocument, UUID> {

    Optional<InvoiceDocument> findByIdAndTenantId(UUID id, UUID tenantId);

    List<InvoiceDocument> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<InvoiceDocument> findByTenantIdAndInvoiceRecordIdOrderByCreatedAtDesc(
            UUID tenantId, UUID invoiceRecordId);

    boolean existsByTenantIdAndInvoiceRecordIdAndSha256(
            UUID tenantId, UUID invoiceRecordId, String sha256);

    boolean existsByTenantIdAndInvoiceRecordIdIsNullAndSha256(UUID tenantId, String sha256);

    /** Retention scan: documents past their purge date that still hold bytes. */
    List<InvoiceDocument> findByPurgeAfterBeforeAndStatusNot(OffsetDateTime cutoff,
            InvoiceDocumentStatus status);
}
