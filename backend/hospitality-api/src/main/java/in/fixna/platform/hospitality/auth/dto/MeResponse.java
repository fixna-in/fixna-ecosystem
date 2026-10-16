package in.fixna.platform.hospitality.auth.dto;

import java.util.UUID;

/**
 * Profile for the authenticated session. Returned by {@code GET /api/v1/me}
 * so the web client can restore user state after a reload or token refresh.
 */
public record MeResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UUID tenantId,
        String role) {
}
