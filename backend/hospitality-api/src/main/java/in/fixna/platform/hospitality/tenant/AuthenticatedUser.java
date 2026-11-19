package in.fixna.platform.hospitality.tenant;

import java.util.UUID;

import in.fixna.platform.tenant.MembershipRole;

public record AuthenticatedUser(UUID userId, UUID tenantId, MembershipRole role) {
}