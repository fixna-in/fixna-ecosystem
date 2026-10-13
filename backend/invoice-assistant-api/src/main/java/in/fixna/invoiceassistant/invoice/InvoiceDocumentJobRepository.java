package in.fixna.invoiceassistant.invoice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for {@link InvoiceDocumentJob}. */
@Repository
public interface InvoiceDocumentJobRepository extends JpaRepository<InvoiceDocumentJob, UUID> {

    Optional<InvoiceDocumentJob> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Find QUEUED jobs to process, oldest first. */
    @Query("""
            SELECT j FROM InvoiceDocumentJob j
            WHERE j.status = :queued
            ORDER BY j.createdAt ASC
            """)
    List<InvoiceDocumentJob> findQueuedJobs(@Param("queued") InvoiceDocumentJobStatus queued);

    /** Claim a QUEUED job for processing: QUEUED → RUNNING. */
    @Modifying
    @Query("""
            UPDATE InvoiceDocumentJob j
               SET j.status = :running, j.startedAt = :now, j.updatedAt = :now
             WHERE j.id = :id AND j.status = :queued
            """)
    int claimJob(@Param("id") UUID id,
                 @Param("now") OffsetDateTime now,
                 @Param("queued") InvoiceDocumentJobStatus queued,
                 @Param("running") InvoiceDocumentJobStatus running);

    /** Mark job as completed successfully. */
    @Modifying
    @Query("""
            UPDATE InvoiceDocumentJob j
               SET j.status = :completed, j.completedAt = :now, j.updatedAt = :now
             WHERE j.id = :id AND j.status = :running
            """)
    int completeJob(@Param("id") UUID id,
                    @Param("now") OffsetDateTime now,
                    @Param("running") InvoiceDocumentJobStatus running,
                    @Param("completed") InvoiceDocumentJobStatus completed);

    /** Mark job as failed. */
    @Modifying
    @Query("""
            UPDATE InvoiceDocumentJob j
               SET j.status = :failed, j.errorCode = :errorCode, j.errorMessage = :errorMessage,
                   j.completedAt = :now, j.updatedAt = :now
             WHERE j.id = :id AND j.status = :running
            """)
    int failJob(@Param("id") UUID id,
                @Param("now") OffsetDateTime now,
                @Param("running") InvoiceDocumentJobStatus running,
                @Param("failed") InvoiceDocumentJobStatus failed,
                @Param("errorCode") String errorCode,
                @Param("errorMessage") String errorMessage);
}
