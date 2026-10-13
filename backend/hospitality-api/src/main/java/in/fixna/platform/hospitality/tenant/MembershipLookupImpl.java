package in.fixna.platform.hospitality.tenant;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class MembershipLookupImpl implements MembershipLookup {

    private final TenantMembershipRepository memberships;

    public MembershipLookupImpl(TenantMembershipRepository memberships) {
        this.memberships = memberships;
    }

    @Override
    public Optional<MembershipRole> roleFor(UUID tenantId, UUID userId) {
        return memberships.findByTenantIdAndUserId(tenantId, userId)
                .map(TenantMembership::getRole)
                .map(MembershipRole::valueOf);
    }
}