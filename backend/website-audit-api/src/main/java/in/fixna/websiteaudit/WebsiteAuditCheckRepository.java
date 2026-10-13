package in.fixna.websiteaudit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebsiteAuditCheckRepository extends JpaRepository<WebsiteAuditCheck, UUID> {

    List<WebsiteAuditCheck> findByAuditIdAndTenantIdOrderByCheckCodeAsc(UUID auditId, UUID tenantId);
}