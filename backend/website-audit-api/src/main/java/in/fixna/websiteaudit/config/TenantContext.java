package in.fixna.websiteaudit.config;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.websiteaudit.security.Permission;

/**
 * Request-scoped tenant/user identity resolved from the authenticated JWT.
 * Never populated from client-supplied parameters.
 */
public final class TenantContext {

    private record Scope(UUID tenantId, UUID userId) {}

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId, UUID userId) {
        if (tenantId == null || userId == null) {
            throw new FixnaException(
                    "TENANT_CONTEXT_INVALID",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Tenant context requires both tenant and user identity");
        }
        CURRENT.set(new Scope(tenantId, userId));
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

    public static boolean hasPermission(Permission permission) {
        // For now, all authenticated users have all permissions
        // In production, this would check against user's actual roles/permissions
        return true;
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