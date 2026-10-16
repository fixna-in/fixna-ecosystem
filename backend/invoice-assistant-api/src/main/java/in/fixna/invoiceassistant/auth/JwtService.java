package in.fixna.invoiceassistant.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /** Exposes the configured JWT properties (TTLs) to callers such as token persistence. */
    public JwtProperties properties() {
        return properties;
    }

    public VerifiedToken verify(String token, String expectedType) {
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();
            if (!expectedType.equals(claims.get("typ", String.class))) {
                throw new InvalidTokenException("Unexpected token type");
            }
            if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
                throw new InvalidTokenException("Token expired");
            }
            if (Instant.now().isAfter(claims.getExpiration().toInstant())) {
                throw new InvalidTokenException("Token expired");
            }
            UUID tenantId = claims.get("tenantId", String.class) != null
                    ? UUID.fromString(claims.get("tenantId", String.class))
                    : null;
            return new VerifiedToken(
                    UUID.fromString(claims.getSubject()), tenantId, claims.get("role", String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid token", ex);
        }
    }

    /** Issues a signed access token (typ=access) with tenant + role claims. */
    public String issueAccessToken(UUID userId, UUID tenantId, String role) {
        return buildToken(userId, tenantId, role, "access", properties.accessTtl());
    }

    /** Issues a signed refresh token (typ=refresh) for rotation. */
    public String issueRefreshToken(UUID userId, UUID tenantId, String role) {
        return buildToken(userId, tenantId, role, "refresh", properties.refreshTtl());
    }

    private String buildToken(UUID userId, UUID tenantId, String role, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                // Unique jti (token id): iat/exp claims are second-granular, so two
                // tokens for the same user/tenant/role issued within the same second
                // would otherwise be byte-equal → identical SHA-256 → duplicate key on
                // refresh_tokens.token_hash (UNIQUE) → 500 on login/refresh rotation.
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("role", role)
                .claim("typ", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public record VerifiedToken(UUID userId, UUID tenantId, String role) {}
}
