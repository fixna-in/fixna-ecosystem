package in.fixna.invoiceassistant.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    /**
     * Regression guard: the CodeQL autofix once flipped CSRF to
     * {@code Customizer.withDefaults()}, which 403s every POST /api/v1/auth/*
     * on a stateless Bearer-token API (no XSRF cookie exchange exists).
     */
    @Test
    void csrfDisabledForStatelessBearerTokenApi() throws Exception {
        String source = Files.readString(securityConfigPath());
        assertThat(source).contains("http.csrf(AbstractHttpConfigurer::disable)");
        assertThat(source).doesNotContain("csrf(Customizer.withDefaults())");
    }

    private static Path securityConfigPath() {
        Path direct = Path.of("src", "main", "java", "in", "fixna", "invoiceassistant",
                "config", "SecurityConfig.java");
        if (Files.exists(direct)) {
            return direct;
        }
        return Path.of("backend", "invoice-assistant-api", "src", "main", "java", "in",
                "fixna", "invoiceassistant", "config", "SecurityConfig.java");
    }
}
