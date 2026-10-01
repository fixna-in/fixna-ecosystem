package in.fixna.platform.consulting.meeting;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    Optional<Meeting> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Meeting> findByTenantIdAndClientIdOrderByStartsAtAsc(UUID tenantId, UUID clientId);

    List<Meeting> findByTenantIdAndProjectIdOrderByStartsAtAsc(UUID tenantId, UUID projectId);

    List<Meeting> findByTenantIdAndClientIdAndProjectIdOrderByStartsAtAsc(
            UUID tenantId, UUID clientId, UUID projectId);

    List<Meeting> findByTenantIdOrderByStartsAtAsc(UUID tenantId);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM Meeting m
            WHERE m.tenantId = :tenantId
              AND m.organizerUserId = :organizerUserId
              AND m.status IN ('SCHEDULED', 'IN_PROGRESS')
              AND m.startsAt < :endsAt
              AND m.endsAt > :startsAt
              AND (:excludeMeetingId IS NULL OR m.id <> :excludeMeetingId)
            """)
    boolean existsOrganizerConflict(
            @Param("tenantId") UUID tenantId,
            @Param("organizerUserId") UUID organizerUserId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("excludeMeetingId") UUID excludeMeetingId);
}
