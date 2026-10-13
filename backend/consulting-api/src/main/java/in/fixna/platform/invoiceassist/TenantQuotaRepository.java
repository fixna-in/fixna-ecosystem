package in.fixna.platform.invoiceassist;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TenantQuota}.
 */
@Repository
public interface TenantQuotaRepository extends JpaRepository<TenantQuota, UUID> {

    Optional<TenantQuota> findById(UUID tenantId);

    @Modifying
    @Query("""
            UPDATE TenantQuota q
               SET q.uploadsThisMonth = :uploads, q.updatedAt = :now
             WHERE q.tenantId = :tenantId
            """)
    int setUploads(@Param("tenantId") UUID tenantId,
                   @Param("uploads") int uploads,
                   @Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
            UPDATE TenantQuota q
               SET q.extractionsThisMonth = :extractions, q.updatedAt = :now
             WHERE q.tenantId = :tenantId
            """)
    int setExtractions(@Param("tenantId") UUID tenantId,
                       @Param("extractions") int extractions,
                       @Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
            UPDATE TenantQuota q
               SET q.uploadsThisMonth = 0, q.extractionsThisMonth = 0, q.resetAt = :resetAt, q.updatedAt = :now
             WHERE q.tenantId = :tenantId
            """)
    int resetQuota(@Param("tenantId") UUID tenantId,
                   @Param("resetAt") OffsetDateTime resetAt,
                   @Param("now") OffsetDateTime now);
}