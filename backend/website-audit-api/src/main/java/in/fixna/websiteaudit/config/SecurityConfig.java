package in.fixna.websiteaudit.config;

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

import in.fixna.websiteaudit.auth.JwtAuthenticationFilter;
import in.fixna.websiteaudit.auth.JwtProperties;
import in.fixna.websiteaudit.security.RateLimitFilter;
import in.fixna.platform.common.web.ApiAccessDeniedHandler;
import in.fixna.platform.common.web.ApiAuthenticationEntryPoint;
import in.fixna.platform.common.web.RequestIdFilter;
import in.fixna.platform.common.web.SecurityHeadersFilter;

@Configuration
@EnableConfigurationProperties({JwtProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestIdFilter requestIdFilter,
            SecurityHeadersFilter securityHeadersFilter,
            RateLimitFilter rateLimitFilter,
            JwtAuthenticationFilter jwtFilter,
            ApiAuthenticationEntryPoint entryPoint,
            ApiAccessDeniedHandler deniedHandler)
            throws Exception {
        // Stateless Bearer-token API (Authorization header, no cookies/sessions):
        // CSRF does not apply. Kept disabled consistently with consulting/localboost/
        // hospitality. The CodeQL "Disabled Spring CSRF protection" finding is a false
        // positive for this design — enabling CSRF here 403s every POST /api/v1/auth/*.
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler))
                .addFilterBefore(requestIdFilter, SecurityContextHolderFilter.class)
                .addFilterBefore(securityHeadersFilter, RequestIdFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/api/v1/health/**",
                                "/api/v1/auth/**",
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
