package in.fixna.platform.identity.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        UUID tenantId,
        String role) {

    public static AuthResponse bearer(
            String accessToken, String refreshToken, long expiresInSeconds,
            UUID userId, UUID tenantId, String role) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, userId, tenantId, role);
    }
}
