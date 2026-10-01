package in.fixna.platform.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import in.fixna.platform.common.config.CorsConfig;
import in.fixna.platform.common.config.CorsProperties;
import in.fixna.platform.common.config.PasswordConfig;

/**
 * Shared Fixna platform beans: error envelope, CORS, security headers, request ID,
 * password encoder. Product services must register a {@link
 * in.fixna.platform.common.tenant.TenantScopeProvider} bridge.
 */
@AutoConfiguration
@EnableConfigurationProperties(CorsProperties.class)
@Import({CorsConfig.class, PasswordConfig.class})
@ComponentScan(basePackages = {
    "in.fixna.platform.common.web",
})
public class FixnaPlatformCommonAutoConfiguration {
}
