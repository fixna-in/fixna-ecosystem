package in.fixna.platform.consulting.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultingServiceRepository extends JpaRepository<ConsultingService, UUID> {

    List<ConsultingService> findByTenantIdOrderBySortOrderAscNameAsc(UUID tenantId);

    List<ConsultingService> findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID tenantId);

    Optional<ConsultingService> findByIdAndTenantId(UUID id, UUID tenantId);
}
