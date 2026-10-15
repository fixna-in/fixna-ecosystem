package in.fixna.invoiceassistant.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import in.fixna.platform.common.web.ApiError;
import in.fixna.platform.common.web.RequestIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Abuse guard for invoice intake writes (WB-09 equivalent): per IP + route
 * sliding window on non-GET {@code /api/v1/invoices/**} and
 * {@code /api/v1/invoice-jobs/**}; auth surface all methods. GET
 * polling/reads stay unlimited so 2s client polling cannot 429.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger RATE_LOG = LoggerFactory.getLogger("fixna.ratelimit");

    private static final String[] AUTH_PREFIXES = {"/api/v1/auth/"};
    private static final String[] WRITE_PREFIXES = {
            "/api/v1/invoice-documents/", "/api/v1/invoices/", "/api/v1/invoice-jobs/"};

    private final int maxHits;
    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(
            @Value("${fixna.security.rate-limit.per-ip-per-minute:20}") int perIpPerMinute) {
        this.maxHits = perIpPerMinute;
    }

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = clientIp(request) + "|" + request.getRequestURI();
        if (!allow(key)) {
            String requestId = MDC.get(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
            RATE_LOG.warn("Rate limit exceeded route={}", request.getRequestURI());
            ApiError error = new ApiError(
                    OffsetDateTime.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests — please retry later",
                    request.getRequestURI(),
                    requestId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), error);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allow(String key) {
        Deque<Long> bucket = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        long now = System.nanoTime();
        long window = Duration.ofMinutes(1).toNanos();
        synchronized (bucket) {
            while (!bucket.isEmpty() && now - bucket.peekFirst() >= window) {
                bucket.removeFirst();
            }
            if (bucket.size() >= maxHits) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
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
