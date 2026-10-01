package in.fixna.platform.consulting.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByClientIdAndTenantIdOrderByCreatedAtDesc(UUID clientId, UUID tenantId);

    List<Invoice> findByClientIdAndTenantIdAndStatusNotOrderByCreatedAtDesc(
            UUID clientId, UUID tenantId, InvoiceStatus excludedStatus);

    Optional<Invoice> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Invoice> findByIdAndTenantIdAndClientId(UUID id, UUID tenantId, UUID clientId);

    long countByTenantId(UUID tenantId);
}
