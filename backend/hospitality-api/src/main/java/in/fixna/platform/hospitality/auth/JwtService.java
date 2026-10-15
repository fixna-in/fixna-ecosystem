package in.fixna.platform.hospitality.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

/**
 * Issue and verify JWT access/refresh tokens.
 * Claims: sub=userId, tid=tenantId, typ=access|refresh.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    public JwtProperties getProps() {
        return props;
    }

    @PostConstruct
    void init() {
        if (props.getSecret() == null || props.getSecret().isBlank()) {
            throw new IllegalStateException("FIXNA_JWT_SECRET must be set");
        }
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes());
    }

    public String issueAccessToken(UUID userId, UUID tenantId) {
        return buildToken(userId, tenantId, "access", props.getAccessTtl());
    }

    public String issueRefreshToken(UUID userId, UUID tenantId) {
        return buildToken(userId, tenantId, "refresh", props.getRefreshTtl());
    }

    private String buildToken(UUID userId, UUID tenantId, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tid", tenantId.toString())
                .claim("typ", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public VerifiedToken verify(String token, String expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new InvalidTokenException("Invalid token", ex);
        }
        String type = claims.get("typ", String.class);
        if (!expectedType.equals(type)) {
            throw new InvalidTokenException("Token type mismatch: expected " + expectedType);
        }
        UUID userId = UUID.fromString(claims.getSubject());
        String tid = claims.get("tid", String.class);
        UUID tenantId = tid != null ? UUID.fromString(tid) : null;
        return new VerifiedToken(userId, tenantId, type, claims.getExpiration().toInstant());
    }

    public record VerifiedToken(UUID userId, UUID tenantId, String type, Instant expiresAt) {}

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
        public InvalidTokenException(String message, Throwable cause) { super(message, cause); }
    }
}