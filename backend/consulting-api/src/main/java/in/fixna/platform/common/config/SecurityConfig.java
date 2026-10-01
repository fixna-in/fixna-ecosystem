package in.fixna.platform.common.config;

import in.fixna.platform.identity.auth.JwtAuthenticationFilter;
import in.fixna.platform.identity.auth.JwtProperties;
import in.fixna.platform.common.web.ApiAccessDeniedHandler;
import in.fixna.platform.common.web.ApiAuthenticationEntryPoint;
import in.fixna.platform.common.web.RequestIdFilter;
import in.fixna.platform.common.web.SecurityHeadersFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableConfigurationProperties({JwtProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestIdFilter requestIdFilter,
            SecurityHeadersFilter securityHeadersFilter,
            JwtAuthenticationFilter jwtFilter,
            ApiAuthenticationEntryPoint entryPoint,
            ApiAccessDeniedHandler deniedHandler)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .addFilterBefore(requestIdFilter, SecurityContextHolderFilter.class)
                .addFilterBefore(securityHeadersFilter, RequestIdFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/portal/auth/login",
                                "/api/v1/public/**",
                                "/api/v1/health/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/api/v1/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll());
        return http.build();
    }
}
