package in.fixna.websiteaudit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebsiteAuditRepository extends JpaRepository<WebsiteAudit, UUID> {

    Optional<WebsiteAudit> findByIdAndTenantId(UUID id, UUID tenantId);

    List<WebsiteAudit> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}