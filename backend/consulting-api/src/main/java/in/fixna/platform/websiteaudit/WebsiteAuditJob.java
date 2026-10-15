package in.fixna.platform.websiteaudit;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Async crawl job with lease-based claim and recovery (ADR-009, V12). */
@Entity
@Table(name = "website_audit_jobs")
@Getter
@Setter
@NoArgsConstructor
public class WebsiteAuditJob {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "audit_id")
    private UUID auditId;

    @Column(name = "requested_url", nullable = false, columnDefinition = "TEXT")
    private String requestedUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebsiteAuditJobStatus status = WebsiteAuditJobStatus.QUEUED;

    @Column(name = "crawl_enabled", nullable = false)
    private boolean crawlEnabled = true;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "lease_expires_at")
    private OffsetDateTime leaseExpiresAt;

    @Column(name = "failure_code", length = 60)
    private String failureCode;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}