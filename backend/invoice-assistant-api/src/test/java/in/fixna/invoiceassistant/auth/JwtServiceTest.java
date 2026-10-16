package in.fixna.invoiceassistant.auth;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-at-least-32-bytes-long!!";

    private final JwtService jwt = new JwtService(
            new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7)));

    /**
     * Regression guard: without a unique jti, refresh tokens issued for the same
     * user/tenant/role within the same second are byte-identical (iat/exp are
     * second-granular), colliding on refresh_tokens.token_hash (UNIQUE) and 500ing
     * login/refresh.
     */
    @Test
    void refreshTokensIssuedInTheSameSecondAreUnique() {
        UUID user = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();

        String first = jwt.issueRefreshToken(user, tenant, "TENANT_OWNER");
        String second = jwt.issueRefreshToken(user, tenant, "TENANT_OWNER");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void issuedTokensVerifyWithExpectedType() {
        UUID user = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();

        String refresh = jwt.issueRefreshToken(user, tenant, "TENANT_OWNER");
        JwtService.VerifiedToken verified = jwt.verify(refresh, "refresh");

        assertThat(verified.userId()).isEqualTo(user);
        assertThat(verified.tenantId()).isEqualTo(tenant);
        assertThat(verified.role()).isEqualTo("TENANT_OWNER");
    }
}
