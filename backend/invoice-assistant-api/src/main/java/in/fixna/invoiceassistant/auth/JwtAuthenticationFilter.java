package in.fixna.invoiceassistant.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import in.fixna.invoiceassistant.auth.JwtService.VerifiedToken;
import in.fixna.invoiceassistant.config.TenantContext;
import in.fixna.invoiceassistant.membership.TenantMembershipRepository;
import in.fixna.platform.common.web.ApiError;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.common.web.RequestIdFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final TenantMembershipRepository memberships;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtService jwt, TenantMembershipRepository memberships) {
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
        VerifiedToken verified;
        try {
            verified = jwt.verify(token, "access");
        } catch (InvalidTokenException ex) {
            throw new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Invalid or expired token", ex);
        }
        if (verified.tenantId() == null) {
            throw new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Token has no tenant scope");
        }
        UUID tenantId = verified.tenantId();
        UUID userId = verified.userId();
        memberships
                .findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new FixnaException(
                        "FORBIDDEN", HttpStatus.FORBIDDEN, "Membership was revoked"));
        TenantContext.set(tenantId, userId);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/health")
                || path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }
}
