package in.fixna.platform.consulting.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findByProjectIdAndTenantIdOrderBySortOrderAsc(UUID projectId, UUID tenantId);

    Optional<Milestone> findByIdAndTenantId(UUID id, UUID tenantId);
}
