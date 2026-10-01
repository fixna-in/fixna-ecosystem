package in.fixna.platform.consulting.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Project> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT p FROM Project p, Engagement e
            WHERE p.engagementId = e.id
              AND p.tenantId = :tenantId
              AND e.clientId = :clientId
            ORDER BY p.createdAt DESC
            """)
    List<Project> findByClientIdAndTenantIdOrderByCreatedAtDesc(
            @Param("clientId") UUID clientId, @Param("tenantId") UUID tenantId);
}
