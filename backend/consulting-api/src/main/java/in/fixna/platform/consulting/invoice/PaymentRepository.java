package in.fixna.platform.consulting.invoice;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceIdAndTenantIdOrderByPaidAtDesc(UUID invoiceId, UUID tenantId);
}
