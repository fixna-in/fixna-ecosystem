package in.fixna.platform.identity.auth;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.logging.LoggingConstants;
import in.fixna.platform.common.logging.LoggingContext;
import in.fixna.platform.common.logging.SensitiveDataMasker;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.identity.User;
import in.fixna.platform.identity.UserRepository;
import in.fixna.platform.consulting.client.ClientPortalMembership;
import in.fixna.platform.consulting.client.ClientPortalMembershipRepository;
import in.fixna.platform.identity.auth.dto.AuthResponse;
import in.fixna.platform.identity.auth.dto.LoginRequest;
import in.fixna.platform.identity.auth.dto.PortalLoginRequest;
import in.fixna.platform.identity.auth.dto.RegisterRequest;
import in.fixna.platform.membership.TenantMembership;
import in.fixna.platform.membership.TenantMembershipRepository;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.Tenant;
import in.fixna.platform.tenancy.TenantRepository;
import in.fixna.platform.tenancy.TenantType;

@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final ClientPortalMembershipRepository portalMemberships;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final JwtProperties jwtProperties;
    private final AuditPublisher audit;

    public AuthService(
            UserRepository users,
            TenantRepository tenants,
            TenantMembershipRepository memberships,
            ClientPortalMembershipRepository portalMemberships,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwt,
            JwtProperties jwtProperties,
            AuditPublisher audit) {
        this.users = users;
        this.tenants = tenants;
        this.memberships = memberships;
        this.portalMemberships = portalMemberships;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.jwtProperties = jwtProperties;
        this.audit = audit;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.normalizedEmail();
        if (users.existsByEmail(email)) {
            LOG.warn("Registration rejected reason=EMAIL_TAKEN userIdentifierHash={}",
                    SensitiveDataMasker.userIdentifierHash(email));
            throw new FixnaException("EMAIL_TAKEN", HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        users.save(user);

        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName().trim());
        tenant.setTenantType(TenantType.CONSULTING_WORKSPACE);
        tenants.save(tenant);

        TenantMembership membership = new TenantMembership();
        membership.setTenantId(tenant.getId());
        membership.setUserId(user.getId());
        membership.setRole(Role.CONSULTANT_ADMIN);
        memberships.save(membership);

        audit.publish(new AuditEvent(
                "auth.registered", tenant.getId(), user.getId(), "user", user.getId().toString(),
                Map.of("tenant", tenant.getId().toString()), null));
        LoggingContext.putOperation(LoggingConstants.AUTH_LOGIN);
        LoggingContext.putTenantAndUser(tenant.getId(), user.getId());
        LOG.info("User registered tenantId={} userId={}", tenant.getId(), user.getId());
        return issueTokens(user.getId(), tenant.getId(), Role.CONSULTANT_ADMIN);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String loginHash = SensitiveDataMasker.userIdentifierHash(request.normalizedEmail());
        User user = users.findByEmail(request.normalizedEmail())
                .orElseThrow(() -> {
                    LOG.warn("Authentication failed reason=INVALID_CREDENTIALS userIdentifierHash={}", loginHash);
                    return new FixnaException(
                            "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password");
                });
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            LOG.warn("Authentication failed reason=INVALID_CREDENTIALS userIdentifierHash={}", loginHash);
            throw new FixnaException(
                    "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        List<TenantMembership> owned = memberships.findByUserId(user.getId());
        owned.sort((a, b) -> {
            boolean aAdmin = a.getRole() == Role.CONSULTANT_ADMIN;
            boolean bAdmin = b.getRole() == Role.CONSULTANT_ADMIN;
            if (aAdmin && !bAdmin) {
                return -1;
            }
            if (bAdmin && !aAdmin) {
                return 1;
            }
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });
        if (owned.isEmpty()) {
            throw new FixnaException("NO_MEMBERSHIP", HttpStatus.FORBIDDEN, "User has no tenant membership");
        }
        TenantMembership selected = owned.get(0);
        LoggingContext.putOperation(LoggingConstants.AUTH_LOGIN);
        LoggingContext.putTenantAndUser(selected.getTenantId(), user.getId());
        LOG.info("Authentication succeeded tenantId={} userId={}", selected.getTenantId(), user.getId());
        audit.publish(new AuditEvent(
                "auth.login", selected.getTenantId(), user.getId(), "user", user.getId().toString(),
                Map.of(), null));
        return issueTokens(user.getId(), selected.getTenantId(), selected.getRole());
    }

    @Transactional
    public AuthResponse portalLogin(PortalLoginRequest request) {
        String loginHash = SensitiveDataMasker.userIdentifierHash(request.normalizedEmail());
        User user = users.findByEmail(request.normalizedEmail())
                .orElseThrow(() -> {
                    LOG.warn("Portal authentication failed reason=INVALID_CREDENTIALS userIdentifierHash={}", loginHash);
                    return new FixnaException(
                            "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password");
                });
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            LOG.warn("Portal authentication failed reason=INVALID_CREDENTIALS userIdentifierHash={}", loginHash);
            throw new FixnaException(
                    "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        ClientPortalMembership membership = portalMemberships
                .findByClientIdAndUserId(request.clientId(), user.getId())
                .orElseThrow(() -> new FixnaException(
                        "NO_PORTAL_MEMBERSHIP",
                        HttpStatus.FORBIDDEN,
                        "User is not a member of this client portal"));
        LoggingContext.putOperation(LoggingConstants.AUTH_LOGIN);
        LoggingContext.putTenantAndUser(membership.getTenantId(), user.getId());
        LOG.info(
                "Portal authentication succeeded tenantId={} userId={} clientId={}",
                membership.getTenantId(),
                user.getId(),
                request.clientId());
        audit.publish(new AuditEvent(
                "auth.portal_login",
                membership.getTenantId(),
                user.getId(),
                "client",
                request.clientId().toString(),
                Map.of("clientId", request.clientId().toString()),
                null));
        return issueTokens(user.getId(), membership.getTenantId(), membership.getRole(), request.clientId());
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        JwtService.VerifiedToken verified;
        try {
            verified = jwt.verify(rawRefreshToken, "refresh");
        } catch (InvalidTokenException ex) {
            throw new FixnaException(
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid", ex);
        }
        RefreshToken stored = refreshTokens
                .findByTokenHashAndRevokedFalse(RefreshToken.hash(rawRefreshToken))
                .orElseThrow(() -> new FixnaException(
                        "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));
        if (stored.getExpiresAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new FixnaException(
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is expired");
        }
        if (!stored.getUserId().equals(verified.userId())) {
            throw new FixnaException(
                    "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }
        stored.setRevoked(true);
        refreshTokens.save(stored);
        if (stored.getClientId() != null) {
            ClientPortalMembership portalMembership = portalMemberships
                    .findByClientIdAndUserIdAndTenantId(
                            stored.getClientId(), stored.getUserId(), stored.getTenantId())
                    .orElseThrow(() -> new FixnaException(
                            "NO_MEMBERSHIP", HttpStatus.FORBIDDEN, "Portal membership was revoked"));
            return issueTokens(
                    stored.getUserId(),
                    stored.getTenantId(),
                    portalMembership.getRole(),
                    stored.getClientId());
        }
        TenantMembership membership = memberships
                .findByTenantIdAndUserId(stored.getTenantId(), stored.getUserId())
                .orElseThrow(() -> new FixnaException(
                        "NO_MEMBERSHIP", HttpStatus.FORBIDDEN, "Membership was revoked"));
        return issueTokens(stored.getUserId(), stored.getTenantId(), membership.getRole());
    }

    @Transactional
    public void logout(UUID userId, UUID tenantId) {
        refreshTokens.revokeAllForUser(userId);
        LoggingContext.putOperation(LoggingConstants.AUTH_LOGOUT);
        LOG.info("Logout completed tenantId={} userId={}", tenantId, userId);
        audit.publish(new AuditEvent(
                "auth.logout", tenantId, userId, "user", userId.toString(), Map.of(), null));
    }

    private AuthResponse issueTokens(UUID userId, UUID tenantId, Role role) {
        return issueTokens(userId, tenantId, role, null);
    }

    private AuthResponse issueTokens(UUID userId, UUID tenantId, Role role, UUID clientId) {
        String access = jwt.issueAccess(userId, tenantId, role.name(), clientId);
        String refresh = jwt.issueRefresh(userId, tenantId);
        RefreshToken record = new RefreshToken();
        record.setUserId(userId);
        record.setTenantId(tenantId);
        record.setClientId(clientId);
        record.setTokenHash(RefreshToken.hash(refresh));
        record.setExpiresAt(java.time.OffsetDateTime.now().plus(jwtProperties.refreshTtl()));
        record.setRevoked(false);
        refreshTokens.save(record);
        return AuthResponse.bearer(
                access, refresh, jwtProperties.accessTtl().toSeconds(), userId, tenantId, role.name());
    }
}
