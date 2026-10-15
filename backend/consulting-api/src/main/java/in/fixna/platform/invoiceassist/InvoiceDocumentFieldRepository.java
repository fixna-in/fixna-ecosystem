package in.fixna.platform.invoiceassist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceDocumentFieldRepository extends JpaRepository<InvoiceDocumentField, UUID> {

    List<InvoiceDocumentField> findByExtractionIdAndTenantIdOrderByFieldKeyAsc(
            UUID extractionId, UUID tenantId);

    Optional<InvoiceDocumentField> findByIdAndTenantId(UUID id, UUID tenantId);

    void deleteByExtractionId(UUID extractionId);
}