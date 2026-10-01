package in.fixna.platform.identity.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fixna.security.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            secret = "dev-only-secret-change-me-in-production-32bytes-minimum!!";
        }
        if (accessTtl == null) {
            accessTtl = Duration.ofMinutes(15);
        }
        if (refreshTtl == null) {
            refreshTtl = Duration.ofDays(7);
        }
    }
}
