package in.fixna.platform.billing;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Subscription lookup. One row per tenant (V6 UNIQUE constraint). */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByTenantId(UUID tenantId);
}