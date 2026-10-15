package in.fixna.platform.websiteaudit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "website_audit_checks")
@Getter
@Setter
@NoArgsConstructor
public class WebsiteAuditCheck {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "audit_id", nullable = false)
    private UUID auditId;

    @Column(name = "check_code", nullable = false, length = 80)
    private String checkCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditSeverity severity = AuditSeverity.INFO;

    @Column(nullable = false)
    private int score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> evidence = Map.of();

    @Column(name = "fix_hint", columnDefinition = "TEXT")
    private String fixHint;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (evidence == null) {
            evidence = Map.of();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
