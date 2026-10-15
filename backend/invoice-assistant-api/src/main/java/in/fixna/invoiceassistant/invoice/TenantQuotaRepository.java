package in.fixna.invoiceassistant.invoice;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantQuotaRepository extends JpaRepository<TenantQuota, UUID> {

    @Override
    Optional<TenantQuota> findById(UUID tenantId);

    /** Reset counters and push the reset window one month forward. */
    @Modifying
    @Query("""
            UPDATE TenantQuota q
               SET q.uploadsThisMonth = 0, q.extractionsThisMonth = 0,
                   q.resetAt = :nextReset, q.updatedAt = :now
             WHERE q.tenantId = :tenantId
            """)
    void resetQuota(@Param("tenantId") UUID tenantId,
                    @Param("nextReset") OffsetDateTime nextReset,
                    @Param("now") OffsetDateTime now);
}
