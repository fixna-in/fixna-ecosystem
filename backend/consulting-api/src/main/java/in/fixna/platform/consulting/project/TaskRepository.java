package in.fixna.platform.consulting.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProjectIdAndTenantIdOrderByCreatedAtAsc(UUID projectId, UUID tenantId);

    Optional<Task> findByIdAndTenantId(UUID id, UUID tenantId);
}
