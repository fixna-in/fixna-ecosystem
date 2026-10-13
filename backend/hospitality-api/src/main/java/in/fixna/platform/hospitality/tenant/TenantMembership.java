package in.fixna.platform.hospitality.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenant_memberships", uniqueConstraints = @UniqueConstraint(name = "uk_membership_tenant_user", columnNames = {"tenant_id", "user_id"}), indexes = {
        @Index(name = "idx_membership_user", columnList = "user_id"),
        @Index(name = "idx_membership_tenant", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMembership {

    @Id
    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private String role;
    private OffsetDateTime createdAt;
}