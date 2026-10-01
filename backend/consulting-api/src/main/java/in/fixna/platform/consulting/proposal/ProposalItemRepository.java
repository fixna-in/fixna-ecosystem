package in.fixna.platform.consulting.proposal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalItemRepository extends JpaRepository<ProposalItem, UUID> {

    List<ProposalItem> findByProposalIdAndTenantIdOrderBySortOrderAsc(UUID proposalId, UUID tenantId);

    Optional<ProposalItem> findByIdAndProposalIdAndTenantId(UUID id, UUID proposalId, UUID tenantId);

    long countByProposalIdAndTenantId(UUID proposalId, UUID tenantId);

    void deleteByIdAndProposalIdAndTenantId(UUID id, UUID proposalId, UUID tenantId);
}
