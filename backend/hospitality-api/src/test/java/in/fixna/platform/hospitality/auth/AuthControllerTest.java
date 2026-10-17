package in.fixna.platform.hospitality.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import in.fixna.platform.hospitality.auth.dto.AuthResponse;
import in.fixna.platform.hospitality.auth.dto.LoginRequest;
import in.fixna.platform.hospitality.auth.dto.RegisterRequest;
import in.fixna.platform.hospitality.tenant.Tenant;
import in.fixna.platform.hospitality.tenant.TenantMembership;
import in.fixna.platform.hospitality.tenant.TenantMembershipRepository;
import in.fixna.platform.hospitality.tenant.TenantRepository;
import in.fixna.platform.hospitality.tenant.User;
import in.fixna.platform.hospitality.tenant.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Timestamps regression: {@code created_at}/{@code updated_at} are NOT NULL in
 * {@code V1__platform_core.sql}, and the DDL {@code DEFAULT now()} never applies
 * because Hibernate inserts explicit NULLs for unset mapped fields. Every entity
 * built by AuthController must therefore carry its timestamps — mirrors the
 * website-audit/invoice-assistant AuthController convention.
 */
class AuthControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final TenantMembershipRepository memberships = mock(TenantMembershipRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);

    private AuthController controller() {
        // Real JwtProperties: defaults are access 15m / refresh 7d.
        when(jwt.getProps()).thenReturn(new JwtProperties());
        return new AuthController(users, tenants, memberships, encoder, jwt, refreshTokens);
    }

    @Test
    void registerSetsTimestampsOnAllPersistedEntities() {
        when(users.existsByEmail("owner@example.com")).thenReturn(false);
        when(encoder.encode("secret123")).thenReturn("hash");
        when(jwt.issueAccessToken(any(), any())).thenReturn("access-token");
        when(jwt.issueRefreshToken(any(), any())).thenReturn("refresh-token");

        ResponseEntity<AuthResponse> response = controller().register(
                new RegisterRequest("owner@example.com", "secret123", "Ada", "Lovelace"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenants).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getCreatedAt()).isNotNull();
        assertThat(tenantCaptor.getValue().getUpdatedAt()).isNotNull();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getCreatedAt()).isNotNull();
        assertThat(userCaptor.getValue().getUpdatedAt()).isNotNull();

        ArgumentCaptor<TenantMembership> membershipCaptor = ArgumentCaptor.forClass(TenantMembership.class);
        verify(memberships).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getCreatedAt()).isNotNull();

        assertThat(savedRefreshToken().getCreatedAt()).isNotNull();
    }

    @Test
    void loginSetsCreatedAtOnRefreshToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("owner@example.com")
                .passwordHash("hash")
                .build();
        when(users.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("secret123", "hash")).thenReturn(true);
        when(memberships.findByUserId(userId)).thenReturn(List.of(TenantMembership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .userId(userId)
                .role("TENANT_OWNER")
                .build()));
        when(jwt.issueAccessToken(any(), any())).thenReturn("access-token");
        when(jwt.issueRefreshToken(any(), any())).thenReturn("refresh-token");

        ResponseEntity<AuthResponse> response = controller()
                .login(new LoginRequest("owner@example.com", "secret123"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(savedRefreshToken().getCreatedAt()).isNotNull();
    }

    private RefreshToken savedRefreshToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(captor.capture());
        return captor.getValue();
    }
}
