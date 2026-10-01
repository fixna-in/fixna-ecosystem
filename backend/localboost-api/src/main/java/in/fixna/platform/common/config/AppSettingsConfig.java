package in.fixna.platform.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the non-security typed settings records (Workflow 13).
 * JwtProperties stays wired in SecurityConfig; CorsProperties is registered
 * by shared platform auto-configuration. This config makes AI/platform
 * settings available for injection app-wide.
 */
@Configuration
@EnableConfigurationProperties({AiSettings.class, PlatformSettings.class})
public class AppSettingsConfig {
}
