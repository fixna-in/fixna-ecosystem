package in.fixna.websiteaudit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebsiteAuditJobRepository extends JpaRepository<WebsiteAuditJob, UUID> {

    Optional<WebsiteAuditJob> findByIdAndTenantId(UUID id, UUID tenantId);

    List<WebsiteAuditJob> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<WebsiteAuditJob> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, WebsiteAuditJobStatus status);

    Optional<WebsiteAuditJob> findFirstByTenantIdAndStatus(UUID tenantId, WebsiteAuditJobStatus status);

    Optional<WebsiteAuditJob> findFirstByStatusOrderByCreatedAtDesc(WebsiteAuditJobStatus status);

    /** CAS claim: QUEUED → RUNNING for this worker; increments the attempt. */
    @Modifying
    @Query("""
            UPDATE WebsiteAuditJob j
               SET j.status = :running, j.lockedBy = :workerId,
                   j.leaseExpiresAt = :leaseUntil, j.attempt = j.attempt + 1, j.updatedAt = :now
             WHERE j.id = :id AND j.status = :queued
            """)
    int claim(@Param("id") UUID id, @Param("workerId") String workerId,
              @Param("leaseUntil") OffsetDateTime leaseUntil, @Param("now") OffsetDateTime now,
              @Param("queued") WebsiteAuditJobStatus queued,
              @Param("running") WebsiteAuditJobStatus running);

    /** Heartbeat: extend the lease while still the active worker. */
    @Modifying
    @Query("""
            UPDATE WebsiteAuditJob j
               SET j.leaseExpiresAt = :leaseUntil, j.updatedAt = :now
             WHERE j.id = :id AND j.status = :running AND j.lockedBy = :workerId
            """)
    int heartbeat(@Param("id") UUID id, @Param("workerId") String workerId,
                  @Param("leaseUntil") OffsetDateTime leaseUntil, @Param("now") OffsetDateTime now,
                  @Param("running") WebsiteAuditJobStatus running);

    /** Recovery: requeue RUNNING jobs whose lease expired (startup + poll). */
    @Modifying
    @Query("""
            UPDATE WebsiteAuditJob j
               SET j.status = :queued, j.lockedBy = null, j.updatedAt = :now
             WHERE j.status = :running
               AND (j.leaseExpiresAt IS NULL OR j.leaseExpiresAt < :now)
            """)
    int requeueExpired(@Param("now") OffsetDateTime now,
                       @Param("queued") WebsiteAuditJobStatus queued,
                       @Param("running") WebsiteAuditJobStatus running);

    /** Terminal transition, conditional on still being the active worker. */
    @Modifying
    @Query("""
            UPDATE WebsiteAuditJob j
               SET j.status = :target, j.failureCode = :failureCode,
                   j.lockedBy = null, j.leaseExpiresAt = null, j.updatedAt = :now
             WHERE j.id = :id AND j.status = :running AND j.lockedBy = :workerId
            """)
    int finishIfLocked(@Param("id") UUID id, @Param("workerId") String workerId,
                       @Param("target") WebsiteAuditJobStatus target,
                       @Param("failureCode") String failureCode, @Param("now") OffsetDateTime now,
                       @Param("running") WebsiteAuditJobStatus running);

    /** Cancel only while not terminal. */
    @Modifying
    @Query("""
            UPDATE WebsiteAuditJob j
               SET j.status = :cancelled, j.lockedBy = null, j.leaseExpiresAt = null, j.updatedAt = :now
             WHERE j.id = :id AND j.status IN :active
            """)
    int cancelIfActive(@Param("id") UUID id, @Param("now") OffsetDateTime now,
                       @Param("cancelled") WebsiteAuditJobStatus cancelled,
                       @Param("active") List<WebsiteAuditJobStatus> active);
}