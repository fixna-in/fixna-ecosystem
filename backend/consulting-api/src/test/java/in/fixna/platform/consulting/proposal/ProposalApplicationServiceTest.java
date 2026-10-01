package in.fixna.platform.consulting.proposal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.engagement.EngagementRepository;
import in.fixna.platform.consulting.proposal.dto.ProposalItemRequest;
import in.fixna.platform.consulting.proposal.dto.ProposalRequest;
import in.fixna.platform.consulting.proposal.dto.ProposalResponse;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProposalApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @Mock ProposalRepository proposals;
    @Mock ProposalItemRepository items;
    @Mock ClientRepository clients;
    @Mock EngagementRepository engagements;
    @Mock DomainEventPublisher events;
    @Mock AuditPublisher audit;

    @InjectMocks ProposalApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asConsultantAdmin() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT_ADMIN);
    }

    private void asConsultant() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT);
    }

    private void asClientAdmin() {
        TenantContext.set(tenantId, userId, Role.CLIENT_ADMIN);
    }

    @Test
    void createPersistsDraftProposalAndPublishesEvent() {
        asConsultantAdmin();
        Client client = new Client();
        client.setId(clientId);
        client.setTenantId(tenantId);
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(proposals.save(any(Proposal.class))).thenAnswer(invocation -> {
            Proposal proposal = invocation.getArgument(0);
            proposal.setId(UUID.randomUUID());
            return proposal;
        });

        ProposalResponse response = service.create(
                new ProposalRequest(clientId, null, "Q1 Advisory Proposal", "Scope notes", "USD", null));

        assertThat(response.title()).isEqualTo("Q1 Advisory Proposal");
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.status()).isEqualTo(ProposalStatus.DRAFT);
        verify(events).publish(any());
        verify(audit).publish(any());
    }

    @Test
    void sendRequiresAtLeastOneLineItem() {
        asConsultantAdmin();
        UUID proposalId = UUID.randomUUID();
        Proposal existing = draftProposal(proposalId);
        when(proposals.findByIdAndTenantId(proposalId, tenantId)).thenReturn(Optional.of(existing));
        when(items.countByProposalIdAndTenantId(proposalId, tenantId)).thenReturn(0L);

        assertThatThrownBy(() -> service.send(proposalId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("PROPOSAL_EMPTY");
    }

    @Test
    void sendMovesDraftToSentWhenItemsExist() {
        asConsultantAdmin();
        UUID proposalId = UUID.randomUUID();
        Proposal existing = draftProposal(proposalId);
        when(proposals.findByIdAndTenantId(proposalId, tenantId)).thenReturn(Optional.of(existing));
        when(items.countByProposalIdAndTenantId(proposalId, tenantId)).thenReturn(1L);
        when(proposals.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findByProposalIdAndTenantIdOrderBySortOrderAsc(proposalId, tenantId)).thenReturn(List.of());

        ProposalResponse response = service.send(proposalId);

        assertThat(response.status()).isEqualTo(ProposalStatus.SENT);
        assertThat(response.sentAt()).isNotNull();
        verify(events).publish(any());
    }

    @Test
    void clientAdminCanApproveSentProposal() {
        asClientAdmin();
        UUID proposalId = UUID.randomUUID();
        Proposal existing = draftProposal(proposalId);
        existing.setStatus(ProposalStatus.SENT);
        when(proposals.findByIdAndTenantId(proposalId, tenantId)).thenReturn(Optional.of(existing));
        when(proposals.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(items.findByProposalIdAndTenantIdOrderBySortOrderAsc(proposalId, tenantId)).thenReturn(List.of());

        ProposalResponse response = service.approve(proposalId);

        assertThat(response.status()).isEqualTo(ProposalStatus.APPROVED);
        verify(events).publish(any());
    }

    @Test
    void consultantCannotApproveProposal() {
        asConsultant();

        assertThatThrownBy(() -> service.approve(UUID.randomUUID()))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sentProposalCannotBeUpdated() {
        asConsultantAdmin();
        UUID proposalId = UUID.randomUUID();
        Proposal existing = draftProposal(proposalId);
        existing.setStatus(ProposalStatus.SENT);
        when(proposals.findByIdAndTenantId(proposalId, tenantId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(
                        proposalId,
                        new ProposalRequest(clientId, null, "Updated title", null, "USD", null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("PROPOSAL_NOT_EDITABLE");
    }

    @Test
    void sentProposalCannotAddItems() {
        asConsultantAdmin();
        UUID proposalId = UUID.randomUUID();
        Proposal existing = draftProposal(proposalId);
        existing.setStatus(ProposalStatus.SENT);
        when(proposals.findByIdAndTenantId(proposalId, tenantId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.addItem(
                        proposalId,
                        new ProposalItemRequest("Line item", BigDecimal.ONE, BigDecimal.TEN, 0)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("PROPOSAL_NOT_EDITABLE");
    }

    @Test
    void missingProposalReturnsNotFound() {
        asConsultantAdmin();
        UUID proposalId = UUID.randomUUID();
        when(proposals.findByIdAndTenantId(proposalId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(proposalId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }

    private Proposal draftProposal(UUID proposalId) {
        Proposal proposal = new Proposal();
        proposal.setId(proposalId);
        proposal.setTenantId(tenantId);
        proposal.setClientId(clientId);
        proposal.setTitle("Proposal");
        proposal.setStatus(ProposalStatus.DRAFT);
        return proposal;
    }
}
