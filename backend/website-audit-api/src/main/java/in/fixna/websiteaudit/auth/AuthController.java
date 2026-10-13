package in.fixna.websiteaudit.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.websiteaudit.auth.dto.AuthResponse;
import in.fixna.websiteaudit.auth.dto.LoginRequest;
import in.fixna.websiteaudit.auth.dto.RefreshRequest;
import in.fixna.websiteaudit.auth.dto.RegisterRequest;
import in.fixna.websiteaudit.membership.TenantMembership;
import in.fixna.websiteaudit.membership.TenantMembershipRepository;
import in.fixna.websiteaudit.tenant.Tenant;
import in.fixna.websiteaudit.tenant.TenantRepository;
import in.fixna.websiteaudit.tenant.User;
import in.fixna.websiteaudit.tenant.UserRepository;
import in.fixna.platform.common.web.FixnaException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String TENANT_TYPE = "WEBSITE_AUDIT";
    static final String OWNER_ROLE = "TENANT_OWNER";

    private final UserRepository users;
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenRepository refreshTokens;

    public AuthController(UserRepository users, TenantRepository tenants,
            TenantMembershipRepository memberships, PasswordEncoder encoder,
            JwtService jwt, RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.tenants = tenants;
        this.memberships = memberships;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
    }

    @Operation(summary = "Register a new tenant and owner user")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new FixnaException("CONFLICT", HttpStatus.CONFLICT, "Email already registered");
        }
        OffsetDateTime now = OffsetDateTime.now();
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name(req.firstName() != null && !req.firstName().isBlank()
                        ? req.firstName() + "'s Workspace"
                        : "New Workspace")
                .tenantType(TENANT_TYPE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        tenants.save(tenant);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .createdAt(now)
                .updatedAt(now)
                .build();
        users.save(user);

        TenantMembership membership = new TenantMembership();
        membership.setId(UUID.randomUUID());
        membership.setTenantId(tenant.getId());
        membership.setUserId(user.getId());
        membership.setRole(OWNER_ROLE);
        membership.setCreatedAt(now);
        memberships.save(membership);

        String access = jwt.issueAccessToken(user.getId(), tenant.getId(), OWNER_ROLE);
        String refresh = jwt.issueRefreshToken(user.getId(), tenant.getId(), OWNER_ROLE);
        saveRefreshToken(user.getId(), tenant.getId(), refresh);
        return ResponseEntity.ok(new AuthResponse(access, refresh, user.getId(), tenant.getId(), OWNER_ROLE));
    }
    @Operation(summary = "Login with email/password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        TenantMembership membership = memberships.findByUserId(user.getId()).stream().findFirst()
                .orElseThrow(() -> new FixnaException("FORBIDDEN", HttpStatus.FORBIDDEN, "No tenant membership"));
        String access = jwt.issueAccessToken(user.getId(), membership.getTenantId(), membership.getRole());
        String refresh = jwt.issueRefreshToken(user.getId(), membership.getTenantId(), membership.getRole());
        saveRefreshToken(user.getId(), membership.getTenantId(), refresh);
        return ResponseEntity.ok(new AuthResponse(
                access, refresh, user.getId(), membership.getTenantId(), membership.getRole()));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        String hash = hashToken(req.refreshToken());
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new FixnaException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }
        TenantMembership membership = memberships.findByTenantIdAndUserId(stored.getTenantId(), stored.getUserId())
                .orElseThrow(() -> new FixnaException("FORBIDDEN", HttpStatus.FORBIDDEN, "Membership was revoked"));
        String access = jwt.issueAccessToken(stored.getUserId(), stored.getTenantId(), membership.getRole());
        String newRefresh = jwt.issueRefreshToken(stored.getUserId(), stored.getTenantId(), membership.getRole());
        refreshTokens.revokeByTokenHash(hash);
        saveRefreshToken(stored.getUserId(), stored.getTenantId(), newRefresh);
        return ResponseEntity.ok(new AuthResponse(
                access, newRefresh, stored.getUserId(), stored.getTenantId(), membership.getRole()));
    }

    private void saveRefreshToken(UUID userId, UUID tenantId, String rawToken) {
        refreshTokens.save(RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tenantId(tenantId)
                .tokenHash(hashToken(rawToken))
                .expiresAt(OffsetDateTime.now().plus(jwt.properties().refreshTtl()))
                .revoked(false)
                .createdAt(OffsetDateTime.now())
                .build());
    }

    private String hashToken(String raw) {
        try {
            return bytesToHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw.getBytes()));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
