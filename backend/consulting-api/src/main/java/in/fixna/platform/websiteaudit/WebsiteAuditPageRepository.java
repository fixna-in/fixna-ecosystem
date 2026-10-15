package in.fixna.platform.websiteaudit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebsiteAuditPageRepository extends JpaRepository<WebsiteAuditPage, UUID> {

    List<WebsiteAuditPage> findByAuditIdAndTenantIdOrderByDepthAsc(UUID auditId, UUID tenantId);

    long countByAuditId(UUID auditId);

    long countByJobId(UUID jobId);

    boolean existsByAuditIdAndUrlHash(UUID auditId, String urlHash);

    void deleteByJobId(UUID jobId);
}