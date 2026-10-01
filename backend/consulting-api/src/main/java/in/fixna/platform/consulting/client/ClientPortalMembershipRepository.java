package in.fixna.platform.consulting.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientPortalMembershipRepository extends JpaRepository<ClientPortalMembership, UUID> {

    List<ClientPortalMembership> findByClientIdAndTenantIdOrderByCreatedAtAsc(UUID clientId, UUID tenantId);

    Optional<ClientPortalMembership> findByClientIdAndUserId(UUID clientId, UUID userId);

    Optional<ClientPortalMembership> findByClientIdAndUserIdAndTenantId(
            UUID clientId, UUID userId, UUID tenantId);
}
