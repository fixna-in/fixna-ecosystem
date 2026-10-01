package in.fixna.platform.consulting.client;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientContactRepository extends JpaRepository<ClientContact, UUID> {

    List<ClientContact> findByClientIdAndTenantIdOrderByNameAsc(UUID clientId, UUID tenantId);
}
