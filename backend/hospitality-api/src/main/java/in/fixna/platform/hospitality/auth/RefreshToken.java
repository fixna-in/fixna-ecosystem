package in.fixna.platform.hospitality.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_user", columnList = "user_id"),
        @Index(name = "idx_refresh_tenant", columnList = "tenant_id"),
        @Index(name = "idx_refresh_expires", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private UUID id;
    private UUID userId;
    private UUID tenantId;
    private String tokenHash;
    private OffsetDateTime expiresAt;
    private boolean revoked;
    private OffsetDateTime createdAt;
}