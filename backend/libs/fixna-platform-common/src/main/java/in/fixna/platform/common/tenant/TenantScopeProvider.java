package in.fixna.platform.common.tenant;

import java.util.UUID;

/**
 * Product-specific tenant/user scope for shared logging and security helpers.
 * Each API service registers an implementation that delegates to its own
 * {@code TenantContext} (Consulting uses {@code in.fixna.platform.tenancy},
 * LocalBoost uses {@code in.fixna.platform.common.tenant}).
 */
public interface TenantScopeProvider {

    UUID tenantIdOrNull();

    UUID userIdOrNull();
}
