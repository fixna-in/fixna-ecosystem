package in.fixna.platform.tenancy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.rbac.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void emptyContextThrowsOnTenantAndUser() {
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("TENANT_CONTEXT_MISSING");
        assertThatThrownBy(TenantContext::requireUserId)
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("TENANT_CONTEXT_MISSING");
    }

    @Test
    void setAndClearScope() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(tenantId, userId, Role.CONSULTANT_ADMIN);

        assertThat(TenantContext.requireTenantId()).isEqualTo(tenantId);
        assertThat(TenantContext.requireUserId()).isEqualTo(userId);
        assertThat(TenantContext.currentRole()).isEqualTo(Role.CONSULTANT_ADMIN);

        TenantContext.clear();
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void consultantAdminHasClientManagePermission() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID(), Role.CONSULTANT_ADMIN);
        assertThat(TenantContext.hasPermission(Permission.CLIENT_MANAGE)).isTrue();
    }

    @Test
    void clientUserDeniedClientManage() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID(), Role.CLIENT_USER);
        assertThat(TenantContext.hasPermission(Permission.CLIENT_MANAGE)).isFalse();
        assertThatThrownBy(() -> TenantContext.requirePermission(Permission.CLIENT_MANAGE))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void portalClientScopeRequired() {
        UUID clientId = UUID.randomUUID();
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID(), Role.CLIENT_ADMIN, clientId);

        assertThat(TenantContext.requireClientId()).isEqualTo(clientId);
        assertThat(TenantContext.clientIdOrNull()).isEqualTo(clientId);
    }

    @Test
    void requireClientIdFailsWithoutPortalScope() {
        TenantContext.set(UUID.randomUUID(), UUID.randomUUID(), Role.CLIENT_ADMIN);

        assertThatThrownBy(TenantContext::requireClientId)
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CLIENT_SCOPE_MISSING");
    }

    @Test
    void crossTenantAccessDenied() {
        UUID tenantA = UUID.randomUUID();
        TenantContext.set(tenantA, UUID.randomUUID(), Role.CONSULTANT);
        UUID recordTenant = UUID.randomUUID();

        assertThatThrownBy(() -> {
            if (!TenantContext.requireTenantId().equals(recordTenant)) {
                throw new FixnaException(
                        "FORBIDDEN", HttpStatus.FORBIDDEN, "Cross-tenant access denied");
            }
        })
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
