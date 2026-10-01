package in.fixna.platform.consulting.client.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.client.ClientPortalMembership;
import in.fixna.platform.rbac.Role;

public record ClientPortalMembershipResponse(
        UUID id,
        UUID clientId,
        UUID userId,
        Role role,
        OffsetDateTime createdAt) {

    public static ClientPortalMembershipResponse from(ClientPortalMembership membership) {
        return new ClientPortalMembershipResponse(
                membership.getId(),
                membership.getClientId(),
                membership.getUserId(),
                membership.getRole(),
                membership.getCreatedAt());
    }
}
