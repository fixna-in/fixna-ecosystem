package in.fixna.platform.consulting.proposal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    List<Proposal> findByClientIdAndTenantIdOrderByCreatedAtDesc(UUID clientId, UUID tenantId);

    List<Proposal> findByClientIdAndTenantIdAndStatusNotInOrderByCreatedAtDesc(
            UUID clientId, UUID tenantId, Collection<ProposalStatus> excludedStatuses);

    Optional<Proposal> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Proposal> findByIdAndTenantIdAndClientId(UUID id, UUID tenantId, UUID clientId);
}
