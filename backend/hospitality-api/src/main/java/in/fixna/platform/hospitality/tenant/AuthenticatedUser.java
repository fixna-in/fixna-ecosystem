package in.fixna.platform.hospitality.tenant;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID tenantId, MembershipRole role) {
}