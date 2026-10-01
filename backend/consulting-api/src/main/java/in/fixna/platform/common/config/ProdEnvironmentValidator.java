package in.fixna.platform.common.config;

import java.util.Arrays;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-fast on {@code prod} when the datasource still points at local defaults.
 */
@Component
public class ProdEnvironmentValidator implements ApplicationRunner {

    static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5433/consulting";
    static final String DEFAULT_DB_PASSWORD = "change-me";

    private final Environment environment;

    public ProdEnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || Arrays.stream(profiles).noneMatch("prod"::equals)) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url", "");
        String username = environment.getProperty("spring.datasource.username", "");
        String password = environment.getProperty("spring.datasource.password", "");

        if (isBlank(url) || url.equals(DEFAULT_DB_URL) || url.contains("localhost")) {
            throw new IllegalStateException(
                    "Refusing to start: spring.datasource.url must point at production PostgreSQL in prod");
        }
        if (isBlank(username)) {
            throw new IllegalStateException(
                    "Refusing to start: spring.datasource.username is required in prod");
        }
        if (isBlank(password) || password.equals(DEFAULT_DB_PASSWORD)) {
            throw new IllegalStateException(
                    "Refusing to start: spring.datasource.password must not use the default in prod");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
