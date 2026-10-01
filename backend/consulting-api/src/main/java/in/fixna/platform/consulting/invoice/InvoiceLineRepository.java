package in.fixna.platform.consulting.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {

    List<InvoiceLine> findByInvoiceIdAndTenantIdOrderBySortOrderAsc(UUID invoiceId, UUID tenantId);

    Optional<InvoiceLine> findByIdAndInvoiceIdAndTenantId(UUID id, UUID invoiceId, UUID tenantId);

    long countByInvoiceIdAndTenantId(UUID invoiceId, UUID tenantId);

    void deleteByIdAndInvoiceIdAndTenantId(UUID id, UUID invoiceId, UUID tenantId);
}
