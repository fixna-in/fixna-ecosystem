package in.fixna.platform.tenancy;

import java.util.UUID;

import in.fixna.platform.rbac.Permission;
import in.fixna.platform.rbac.Role;

public record AuthenticatedUser(UUID userId, UUID tenantId, Role role, UUID clientId) {

    public AuthenticatedUser(UUID userId, UUID tenantId, Role role) {
        this(userId, tenantId, role, null);
    }

    public boolean hasPermission(Permission permission) {
        return TenantContext.hasPermission(permission);
    }
}
