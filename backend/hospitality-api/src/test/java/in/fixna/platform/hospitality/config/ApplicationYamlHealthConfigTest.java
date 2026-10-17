package in.fixna.platform.hospitality.config;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Render deploy fix: the aggregated {@code /api/v1/health} must not
 * include the Redis contributor. Render runs no local Redis, so a DOWN Redis
 * health component made the health check return 503 and the deploy fail
 * (connection refused on localhost:6379). The Redis endpoint stays
 * configurable via {@code REDIS_URL} for deployments that provision one.
 * Mirrors localboost-api {@code application.yml}.
 */
class ApplicationYamlHealthConfigTest {

    @Test
    void redisHealthIsExcludedFromAggregatedHealth() throws IOException {
        assertThat(property("management.health.redis.enabled")).isEqualTo("false");
    }

    @Test
    void redisUrlIsOverridableViaEnvVar() throws IOException {
        assertThat(property("spring.data.redis.url")).isEqualTo("${REDIS_URL:redis://localhost:6379}");
    }

    private static String property(String key) throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        assertThat(sources).hasSize(1);
        // YAML scalars keep their native type (e.g. `false` is a Boolean).
        Object value = sources.get(0).getProperty(key);
        return value == null ? null : String.valueOf(value);
    }
}
