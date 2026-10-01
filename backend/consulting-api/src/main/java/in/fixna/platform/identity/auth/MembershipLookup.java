package in.fixna.platform.identity.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import in.fixna.platform.consulting.client.ClientPortalMembership;
import in.fixna.platform.consulting.client.ClientPortalMembershipRepository;
import in.fixna.platform.membership.TenantMembershipRepository;
import in.fixna.platform.rbac.Role;

@Component
public class MembershipLookup {

    private final TenantMembershipRepository memberships;
    private final ClientPortalMembershipRepository portalMemberships;

    public MembershipLookup(
            TenantMembershipRepository memberships, ClientPortalMembershipRepository portalMemberships) {
        this.memberships = memberships;
        this.portalMemberships = portalMemberships;
    }

    public Optional<Role> roleFor(UUID tenantId, UUID userId) {
        return memberships.findByTenantIdAndUserId(tenantId, userId).map(m -> m.getRole());
    }

    public Optional<Role> portalRoleFor(UUID tenantId, UUID userId, UUID clientId) {
        return portalMemberships
                .findByClientIdAndUserIdAndTenantId(clientId, userId, tenantId)
                .map(ClientPortalMembership::getRole);
    }
}
