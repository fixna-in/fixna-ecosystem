package in.fixna.platform.security;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicEndpointComplianceTest {

    @Test
    void jwtFilterBypassesPublicAndAuthPaths() throws Exception {
        Path filter = sourceFile("identity/auth/JwtAuthenticationFilter.java");
        String source = Files.readString(filter);
        assertThat(source).contains("/api/v1/auth/register");
        assertThat(source).contains("/api/v1/auth/login");
        assertThat(source).contains("/api/v1/auth/refresh");
        assertThat(source).contains("/api/v1/portal/auth/login");
        assertThat(source).contains("/api/v1/public/");
        assertThat(source).contains("/api/v1/health");
    }

    @Test
    void securityConfigPermitsPublicAndAuthPaths() throws Exception {
        Path config = sourceFile("common/config/SecurityConfig.java");
        String source = Files.readString(config);
        assertThat(source).contains("/api/v1/portal/auth/login");
        assertThat(source).contains("/api/v1/public/**");
        assertThat(source).contains("/api/v1/health/**");
    }

    private static Path sourceFile(String relative) {
        Path module = Path.of("").toAbsolutePath();
        Path direct = module.resolve("src").resolve("main").resolve("java").resolve("in").resolve("fixna")
                .resolve("platform")
                .resolve(relative.replace("/", java.io.File.separator));
        if (Files.exists(direct)) {
            return direct;
        }
        return module.resolve("backend")
                .resolve("consulting-api")
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("in")
                .resolve("fixna")
                .resolve("platform")
                .resolve(relative.replace("/", java.io.File.separator));
    }
}
