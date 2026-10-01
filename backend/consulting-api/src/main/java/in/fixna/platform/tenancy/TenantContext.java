package in.fixna.platform.tenancy;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.rbac.RolePermissions;

/**
 * Request-scoped tenant/user identity resolved from the authenticated JWT.
 * Never populated from client-supplied parameters.
 */
public final class TenantContext {

    private record Scope(UUID tenantId, UUID userId, Role role, UUID clientId) {}

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId, UUID userId, Role role) {
        set(tenantId, userId, role, null);
    }

    public static void set(UUID tenantId, UUID userId, Role role, UUID clientId) {
        if (tenantId == null || userId == null) {
            throw new FixnaException(
                    "TENANT_CONTEXT_INVALID",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant context requires both tenant and user identity");
        }
        CURRENT.set(new Scope(tenantId, userId, role == null ? Role.CLIENT_USER : role, clientId));
    }

    public static UUID requireTenantId() {
        Scope scope = CURRENT.get();
        if (scope == null) {
            throw new FixnaException(
                    "TENANT_CONTEXT_MISSING",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant context is not available for this request");
        }
        return scope.tenantId();
    }

    public static UUID requireUserId() {
        Scope scope = CURRENT.get();
        if (scope == null) {
            throw new FixnaException(
                    "TENANT_CONTEXT_MISSING",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant context is not available for this request");
        }
        return scope.userId();
    }

    public static Role currentRole() {
        Scope scope = CURRENT.get();
        return scope == null ? Role.CLIENT_USER : scope.role();
    }

    public static UUID clientIdOrNull() {
        Scope scope = CURRENT.get();
        return scope == null ? null : scope.clientId();
    }

    public static UUID requireClientId() {
        Scope scope = CURRENT.get();
        if (scope == null || scope.clientId() == null) {
            throw new FixnaException(
                    "CLIENT_SCOPE_MISSING",
                    HttpStatus.FORBIDDEN,
                    "Client portal scope is required for this request");
        }
        return scope.clientId();
    }

    public static boolean hasPermission(Permission permission) {
        return RolePermissions.has(currentRole(), permission);
    }

    public static void requirePermission(Permission permission) {
        if (!hasPermission(permission)) {
            throw new FixnaException("FORBIDDEN", HttpStatus.FORBIDDEN, "Permission denied: " + permission);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }
}
