package in.fixna.platform.websiteaudit;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One crawled page persisted as bounded progress (V12, append-only). */
@Entity
@Table(name = "website_audit_pages")
@Getter
@Setter
@NoArgsConstructor
public class WebsiteAuditPage {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "audit_id", nullable = false)
    private UUID auditId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "url_hash", nullable = false, length = 64)
    private String urlHash;

    @Column(nullable = false)
    private int depth;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Integer sizeBytes;

    @Column(name = "fetch_ms")
    private Integer fetchMs;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (fetchedAt == null) {
            fetchedAt = OffsetDateTime.now();
        }
    }
}