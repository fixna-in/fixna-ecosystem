package in.fixna.platform.consulting.testimonial;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestimonialRepository extends JpaRepository<Testimonial, UUID> {

    List<Testimonial> findByTenantIdOrderBySortOrderAscAuthorNameAsc(UUID tenantId);

    List<Testimonial> findByTenantIdAndPublishedTrueOrderBySortOrderAscAuthorNameAsc(UUID tenantId);

    Optional<Testimonial> findByIdAndTenantId(UUID id, UUID tenantId);
}
