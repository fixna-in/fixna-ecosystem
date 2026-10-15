package in.fixna.platform.common.web;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Brute-force / abuse guard: per IP + route sliding window; on exhaustion
 * responds 429 with the standard envelope and a Retry-After hint.
 * X-Forwarded-For is honored for caller IP behind the platform's trusted
 * reverse proxy; rate limiting is a mitigation layer, not the credential
 * control.
 *
 * <p>Covered routes: the public auth surface ({@code /api/v1/auth/**} and
 * {@code /api/v1/portal/auth/**}) for all methods, plus write (non-GET)
 * routes for website-audit jobs and invoice document intake
 * (ADR-009, WB-09): {@code POST /website-audits/jobs},
 * {@code POST /website-audits/jobs/{id}/cancel},
 * {@code POST /invoices/.../documents...}. Read/GET polling
 * ({@code GET /website-audits/jobs/{id}}, report, extraction, search,
 * streaming content) is intentionally not limited so 2s client polling
 * cannot 429 under the default 20/min budget.
 *
 * <p>Adapted from the localboost-api filter so each service stays
 * independently deployable; promote to fixna-platform-common when a third
 * consumer appears.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger RATE_LOG = LoggerFactory.getLogger("fixna.ratelimit");

    private static final String[] AUTH_PREFIXES = {"/api/v1/auth/", "/api/v1/portal/auth/"};

    /**
     * Write-route prefixes for audit jobs + invoice intake (WB-09). Matched
     * only for non-GET methods so polling/reads stay unlimited.
     */
    private static final String[] WRITE_PREFIXES = {
        "/api/v1/website-audits/jobs",
        "/api/v1/invoices/",
        "/api/v1/invoice-jobs/"
    };

    private final RateLimiter limiter;
    private final ObjectMapper mapper;

    /**
     * Spring constructor. Explicitly {@code @Autowired} because the
     * package-visible test seam below gives the class two constructors —
     * without the annotation Spring cannot pick one and fails with
     * "No default constructor found" during full-context boots.
     */
    @Autowired
    public RateLimitFilter(
            @Value("${fixna.security.rate-limit.per-ip-per-minute:20}") int perIpPerMinute) {
        this.limiter = new RateLimiter(perIpPerMinute, Duration.ofMinutes(1));
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    // Package-visible seam for unit tests (custom quota, shared clock reality).
    RateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        for (String prefix : AUTH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return false;
            }
        }
        // WB-09: write routes for audit jobs + invoice intake (POST/PUT/PATCH/
        // DELETE only — GET polling/reads stay unlimited).
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            for (String prefix : WRITE_PREFIXES) {
                if (uri.startsWith(prefix)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientIp(request) + "|" + request.getRequestURI();
        if (!limiter.allow(key)) {
            long retryAfter = limiter.retryAfterSeconds(key);
            String requestId = MDC.get(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
            RATE_LOG.warn("Rate limit exceeded route={} retryAfter={}s", request.getRequestURI(), retryAfter);
            ApiError error = new ApiError(
                    OffsetDateTime.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests — please retry later",
                    request.getRequestURI(),
                    requestId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), error);
            return;
        }
        filterChain.doFilter(request, response);
    }

    // Package-visible seam for tests (no Spring context needed).
    static String keyFor(String clientIp, String route) {
        return clientIp + "|" + route;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",", 2)[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }
}