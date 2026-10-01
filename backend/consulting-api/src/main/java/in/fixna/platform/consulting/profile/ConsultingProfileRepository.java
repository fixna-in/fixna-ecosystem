package in.fixna.platform.consulting.profile;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultingProfileRepository extends JpaRepository<ConsultingProfile, UUID> {

    Optional<ConsultingProfile> findByTenantId(UUID tenantId);

    Optional<ConsultingProfile> findByPublicSlugAndPublishedTrue(String publicSlug);

    boolean existsByPublicSlugAndTenantIdNot(String publicSlug, UUID tenantId);
}
