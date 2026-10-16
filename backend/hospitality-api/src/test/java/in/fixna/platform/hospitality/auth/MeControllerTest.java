package in.fixna.platform.hospitality.auth;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.hospitality.auth.dto.MeResponse;
import in.fixna.platform.hospitality.tenant.MembershipRole;
import in.fixna.platform.hospitality.tenant.TenantContext;
import in.fixna.platform.hospitality.tenant.User;
import in.fixna.platform.hospitality.tenant.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final MeController controller = new MeController(users);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void returnsProfileFromJwtDerivedTenantScope() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(tenantId, userId, MembershipRole.TENANT_OWNER);
        when(users.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId)
                .email("owner@example.com")
                .firstName("Test")
                .lastName("Owner")
                .build()));

        var response = controller.me();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        MeResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(userId);
        assertThat(body.email()).isEqualTo("owner@example.com");
        assertThat(body.firstName()).isEqualTo("Test");
        assertThat(body.lastName()).isEqualTo("Owner");
        assertThat(body.tenantId()).isEqualTo(tenantId);
        assertThat(body.role()).isEqualTo("TENANT_OWNER");
    }

    @Test
    void failsNotFoundWhenUserWasDeleted() {
        UUID userId = UUID.randomUUID();
        TenantContext.set(UUID.randomUUID(), userId, MembershipRole.TENANT_VIEWER);
        when(users.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(controller::me)
                .isInstanceOf(FixnaException.class)
                .hasMessageContaining("User no longer exists");
    }
}
