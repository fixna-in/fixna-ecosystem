package in.fixna.websiteaudit;

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

@Entity
@Table(name = "website_audits")
@Getter
@Setter
@NoArgsConstructor
public class WebsiteAudit {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "input_url", nullable = false, columnDefinition = "TEXT")
    private String inputUrl;

    @Column(name = "normalized_url_hash", nullable = false, length = 64)
    private String normalizedUrlHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebsiteAuditStatus status = WebsiteAuditStatus.COMPLETED;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

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