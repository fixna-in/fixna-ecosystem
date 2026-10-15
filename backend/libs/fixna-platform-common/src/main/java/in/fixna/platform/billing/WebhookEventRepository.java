package in.fixna.platform.billing;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for idempotent webhook event tracking. */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    boolean existsByEventId(String eventId);

    Optional<WebhookEvent> findByEventId(String eventId);
}