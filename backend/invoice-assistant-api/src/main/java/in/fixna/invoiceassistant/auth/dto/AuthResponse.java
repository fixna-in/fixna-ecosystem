package in.fixna.invoiceassistant.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        UUID tenantId,
        String role) {
}
