package in.fixna.invoiceassistant.invoice;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRecordLineRepository extends JpaRepository<InvoiceRecordLine, UUID> {

    List<InvoiceRecordLine> findByInvoiceRecordIdAndTenantIdOrderBySortOrderAsc(
            UUID invoiceRecordId, UUID tenantId);
}
