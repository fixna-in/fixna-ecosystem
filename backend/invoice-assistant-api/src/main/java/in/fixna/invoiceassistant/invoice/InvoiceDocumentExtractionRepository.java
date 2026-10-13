package in.fixna.invoiceassistant.invoice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceDocumentExtractionRepository
        extends JpaRepository<InvoiceDocumentExtraction, UUID> {

    Optional<InvoiceDocumentExtraction> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<InvoiceDocumentExtraction> findFirstByDocumentIdAndTenantIdOrderByCreatedAtDesc(
            UUID documentId, UUID tenantId);

    List<InvoiceDocumentExtraction> findByDocumentIdAndTenantIdOrderByCreatedAtDesc(
            UUID documentId, UUID tenantId);

    /**
     * Search extractions by tenant with optional filters.
     */
    @Query("""
            SELECT e FROM InvoiceDocumentExtraction e
            WHERE e.tenantId = :tenantId
              AND (:status IS NULL OR e.status = :status)
              AND (:dateFrom IS NULL OR e.createdAt >= :dateFrom)
              AND (:dateTo IS NULL OR e.createdAt <= :dateTo)
              AND (:currency IS NULL OR EXISTS (
                  SELECT 1 FROM InvoiceDocumentField f
                  WHERE f.extractionId = e.id AND f.currency = :currency
              ))
              AND (:query IS NULL OR EXISTS (
                  SELECT 1 FROM InvoiceDocumentField f
                  WHERE f.extractionId = e.id
                    AND (f.fieldKey ILIKE %:query% OR f.fieldValue ILIKE %:query%)
              ))
            ORDER BY e.createdAt DESC
            """)
    Page<InvoiceDocumentExtraction> searchByTenant(
            @Param("tenantId") UUID tenantId,
            @Param("status") InvoiceExtractionStatus status,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            @Param("currency") String currency,
            @Param("query") String query,
            Pageable pageable);
}
