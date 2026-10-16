package in.fixna.platform.hospitality.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-fast on {@code prod} when the datasource still points at local defaults.
 * Mirrors the LocalBoost/Consulting guards: the application refuses to start
 * unless PostgreSQL is explicitly configured (never the bundled localhost
 * default) with non-placeholder credentials.
 */
@Component
public class ProdEnvironmentValidator implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ProdEnvironmentValidator.class);

    static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/hospitality";
    static final String LOCAL_DB_URL = "jdbc:postgresql://localhost:5434/hospitality";
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

        if (isBlank(url) || url.equals(DEFAULT_DB_URL) || url.equals(LOCAL_DB_URL) || url.contains("localhost")) {
            throw new IllegalStateException(
                    "Refusing to start: spring.datasource.url must point at production PostgreSQL in prod"
                            + " (SPRING_DATASOURCE_URL) — got: " + maskJdbcUrl(url));
        }
        if (isBlank(username)) {
            throw new IllegalStateException(
                    "Refusing to start: spring.datasource.username is required in prod (POSTGRES_USER)");
        }
        if (isBlank(password) || password.equals(DEFAULT_DB_PASSWORD)) {
            throw new IllegalStateException(
                    "Refusing to start: spring.datasource.password must not use the default in prod"
                            + " (SPRING_DATASOURCE_PASSWORD / POSTGRES_PASSWORD)");
        }
        String jdbcScheme = jdbcScheme(url);
        if (!"postgresql".equalsIgnoreCase(jdbcScheme)) {
            LOG.warn("Datasource URL does not use the jdbc:postgresql scheme (scheme={}) — "
                    + "Render connectionString is postgres:// and must be converted;"
                    + " see render-hospitality.yaml",
                    jdbcScheme);
        }
        LOG.info("Prod datasource check passed (host masked): {}", maskJdbcUrl(url));
    }

    /** Scheme after the {@code jdbc:} prefix, e.g. {@code postgresql} — never logs credentials. */
    static String jdbcScheme(String url) {
        if (url == null || !url.startsWith("jdbc:")) {
            return "";
        }
        String rest = url.substring("jdbc:".length());
        int end = rest.indexOf(':');
        return end < 0 ? rest : rest.substring(0, end);
    }

    /** Masks userinfo/query from a JDBC URL so hosts can be logged without leaking secrets. */
    static String maskJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return "<empty>";
        }
        // jdbc:postgresql://host:port/db?params — strip query params (may carry credentials).
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query) + "?<masked>";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
