package in.fixna.invoiceassistant.invoice;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRecordRepository extends JpaRepository<InvoiceRecord, UUID> {

    Optional<InvoiceRecord> findByIdAndTenantId(UUID id, UUID tenantId);
}
