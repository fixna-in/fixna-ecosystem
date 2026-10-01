package in.fixna.platform.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-fast on the {@code prod} profile when JWT secret or CORS allowlist are unsafe.
 */
@Component
public class ProductionSecurityValidator implements ApplicationRunner {

    static final String DEV_JWT_PLACEHOLDER =
            "dev-only-secret-change-me-in-production-32bytes-minimum!!";
    static final int MIN_SECRET_LENGTH = 32;

    private final Environment environment;

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }
        validateJwtSecret(environment.getProperty("fixna.security.jwt.secret", ""));
        validateCorsOrigins(environment.getProperty("fixna.security.cors.allowed-origins", ""));
    }

    static void validateJwtSecret(String secret) {
        if (secret == null
                || secret.isBlank()
                || secret.equals(DEV_JWT_PLACEHOLDER)
                || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "Refusing to start: FIXNA_JWT_SECRET must be at least 32 characters and not a dev default");
        }
    }

    static void validateCorsOrigins(String origins) {
        if (origins == null || origins.isBlank()) {
            throw new IllegalStateException(
                    "Refusing to start: FIXNA_CORS_ALLOWED_ORIGINS must be set in prod");
        }
        List<String> entries = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        for (String origin : entries) {
            if (origin.contains("localhost") || origin.contains("127.0.0.1")) {
                throw new IllegalStateException(
                        "Refusing to start: FIXNA_CORS_ALLOWED_ORIGINS must not include localhost in prod");
            }
        }
    }

    private boolean isProdProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles != null && Arrays.asList(profiles).contains("prod");
    }
}
