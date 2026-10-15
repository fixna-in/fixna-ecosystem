package in.fixna.platform.hospitality.tenant;

import java.util.Optional;
import java.util.UUID;

public interface MembershipLookup {

    Optional<MembershipRole> roleFor(UUID tenantId, UUID userId);
}