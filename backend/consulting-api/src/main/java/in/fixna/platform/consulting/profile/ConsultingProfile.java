package in.fixna.platform.consulting.profile;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "consulting_profiles")
@Getter
@Setter
@NoArgsConstructor
public class ConsultingProfile {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(length = 500)
    private String tagline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "public_slug", length = 100, unique = true)
    private String publicSlug;

    @Column(name = "is_published", nullable = false)
    private boolean published;

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
