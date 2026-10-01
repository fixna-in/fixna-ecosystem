package in.fixna.platform.identity.auth;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import in.fixna.platform.common.logging.LoggingContext;
import in.fixna.platform.common.web.ApiError;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.common.web.RequestIdFilter;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.AuthenticatedUser;
import in.fixna.platform.tenancy.TenantContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final MembershipLookup memberships;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtService jwt, MembershipLookup memberships) {
        this.jwt = jwt;
        this.memberships = memberships;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, java.io.IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                if (!token.isEmpty()) {
                    authenticate(token);
                    LoggingContext.putTenantAndUser();
                }
            }
            chain.doFilter(request, response);
        } catch (FixnaException ex) {
            ApiError error = new ApiError(
                    OffsetDateTime.now(),
                    ex.getStatus().value(),
                    ex.getCode(),
                    ex.getMessage(),
                    request.getRequestURI(),
                    MDC.get(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
            response.setStatus(ex.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), error);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(String token) {
        JwtService.VerifiedToken verified;
        try {
            verified = jwt.verify(token, "access");
        } catch (InvalidTokenException ex) {
            throw new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Invalid or expired token", ex);
        }
        if (verified.tenantId() == null) {
            throw new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Token has no tenant scope");
        }
        Role role;
        if (verified.clientId() != null) {
            role = memberships
                    .portalRoleFor(verified.tenantId(), verified.userId(), verified.clientId())
                    .orElseThrow(() -> new FixnaException(
                            "FORBIDDEN", HttpStatus.FORBIDDEN, "Portal membership was revoked"));
        } else {
            role = memberships
                    .roleFor(verified.tenantId(), verified.userId())
                    .orElseThrow(() -> new FixnaException(
                            "FORBIDDEN", HttpStatus.FORBIDDEN, "Membership was revoked"));
        }
        TenantContext.set(verified.tenantId(), verified.userId(), role, verified.clientId());
        AuthenticatedUser principal = new AuthenticatedUser(
                verified.userId(), verified.tenantId(), role, verified.clientId());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/portal/auth/login")
                || path.startsWith("/api/v1/public/")
                || path.startsWith("/api/v1/health")
                || path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }
}
