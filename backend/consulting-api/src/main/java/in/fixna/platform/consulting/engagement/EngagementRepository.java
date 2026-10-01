package in.fixna.platform.consulting.engagement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EngagementRepository extends JpaRepository<Engagement, UUID> {

    List<Engagement> findByClientIdAndTenantIdOrderByCreatedAtDesc(UUID clientId, UUID tenantId);

    Optional<Engagement> findByIdAndTenantId(UUID id, UUID tenantId);
}
