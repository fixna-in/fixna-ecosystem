package in.fixna.platform.consulting.portal;

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

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.invoice.Invoice;
import in.fixna.platform.consulting.invoice.InvoiceLineRepository;
import in.fixna.platform.consulting.invoice.InvoiceRepository;
import in.fixna.platform.consulting.invoice.InvoiceStatus;
import in.fixna.platform.consulting.invoice.PaymentRepository;
import in.fixna.platform.consulting.meeting.MeetingRepository;
import in.fixna.platform.consulting.portal.dto.PortalMeResponse;
import in.fixna.platform.consulting.project.Project;
import in.fixna.platform.consulting.project.ProjectRepository;
import in.fixna.platform.consulting.proposal.Proposal;
import in.fixna.platform.consulting.proposal.ProposalApplicationService;
import in.fixna.platform.consulting.proposal.ProposalItemRepository;
import in.fixna.platform.consulting.proposal.ProposalRepository;
import in.fixna.platform.consulting.proposal.ProposalStatus;
import in.fixna.platform.consulting.proposal.dto.ProposalResponse;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    @Mock ClientRepository clients;
    @Mock ProjectRepository projects;
    @Mock ProposalRepository proposals;
    @Mock ProposalItemRepository proposalItems;
    @Mock ProposalApplicationService proposalService;
    @Mock InvoiceRepository invoices;
    @Mock InvoiceLineRepository invoiceLines;
    @Mock PaymentRepository payments;
    @Mock MeetingRepository meetings;

    @InjectMocks PortalApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asClientAdmin() {
        TenantContext.set(tenantId, userId, Role.CLIENT_ADMIN, clientId);
    }

    private void asClientUser() {
        TenantContext.set(tenantId, userId, Role.CLIENT_USER, clientId);
    }

    @Test
    void meReturnsClientContext() {
        asClientAdmin();
        Client client = new Client();
        client.setId(clientId);
        client.setTenantId(tenantId);
        client.setName("Acme Corp");
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));

        PortalMeResponse me = service.me();

        assertThat(me.clientId()).isEqualTo(clientId);
        assertThat(me.clientName()).isEqualTo("Acme Corp");
        assertThat(me.role()).isEqualTo(Role.CLIENT_ADMIN);
    }

    @Test
    void meRequiresPortalClientScope() {
        TenantContext.set(tenantId, userId, Role.CLIENT_ADMIN);

        assertThatThrownBy(() -> service.me())
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CLIENT_SCOPE_MISSING");
    }

    @Test
    void listProposalsHidesDraftAndCancelled() {
        asClientUser();
        when(proposals.findByClientIdAndTenantIdAndStatusNotInOrderByCreatedAtDesc(
                        eq(clientId), eq(tenantId), any()))
                .thenReturn(List.of());

        service.listProposals();

        verify(proposals).findByClientIdAndTenantIdAndStatusNotInOrderByCreatedAtDesc(
                clientId, tenantId, List.of(ProposalStatus.DRAFT, ProposalStatus.CANCELLED));
    }

    @Test
    void getProposalRejectsHiddenStatus() {
        asClientUser();
        UUID proposalId = UUID.randomUUID();
        Proposal draft = new Proposal();
        draft.setId(proposalId);
        draft.setTenantId(tenantId);
        draft.setClientId(clientId);
        draft.setStatus(ProposalStatus.DRAFT);
        when(proposals.findByIdAndTenantIdAndClientId(proposalId, tenantId, clientId))
                .thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.getProposal(proposalId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getProposalRejectsOtherClient() {
        asClientUser();
        UUID proposalId = UUID.randomUUID();
        when(proposals.findByIdAndTenantIdAndClientId(proposalId, tenantId, clientId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProposal(proposalId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listInvoicesHidesDraft() {
        asClientUser();
        when(invoices.findByClientIdAndTenantIdAndStatusNotOrderByCreatedAtDesc(
                        clientId, tenantId, InvoiceStatus.DRAFT))
                .thenReturn(List.of());

        service.listInvoices();

        verify(invoices).findByClientIdAndTenantIdAndStatusNotOrderByCreatedAtDesc(
                clientId, tenantId, InvoiceStatus.DRAFT);
    }

    @Test
    void listProjectsScopedToClient() {
        asClientUser();
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTenantId(tenantId);
        project.setEngagementId(UUID.randomUUID());
        project.setName("Portal project");
        when(projects.findByClientIdAndTenantIdOrderByCreatedAtDesc(clientId, tenantId))
                .thenReturn(List.of(project));

        assertThat(service.listProjects()).hasSize(1);
        verify(projects).findByClientIdAndTenantIdOrderByCreatedAtDesc(clientId, tenantId);
        verify(projects, never()).findByTenantIdOrderByCreatedAtDesc(any());
    }

    @Test
    void clientUserCannotApproveProposal() {
        asClientUser();
        UUID proposalId = UUID.randomUUID();

        assertThatThrownBy(() -> service.approveProposal(proposalId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(proposalService, never()).approve(any());
        verify(proposals, never()).findByIdAndTenantIdAndClientId(any(), any(), any());
    }

    @Test
    void clientAdminCanApproveProposal() {
        asClientAdmin();
        UUID proposalId = UUID.randomUUID();
        Proposal sent = new Proposal();
        sent.setId(proposalId);
        sent.setTenantId(tenantId);
        sent.setClientId(clientId);
        sent.setStatus(ProposalStatus.SENT);
        when(proposals.findByIdAndTenantIdAndClientId(proposalId, tenantId, clientId))
                .thenReturn(Optional.of(sent));
        ProposalResponse approved = new ProposalResponse(
                proposalId,
                clientId,
                null,
                "Q1",
                null,
                ProposalStatus.APPROVED,
                null,
                null,
                null,
                "USD",
                null,
                null,
                List.of(),
                null,
                null);
        when(proposalService.approve(proposalId)).thenReturn(approved);

        assertThat(service.approveProposal(proposalId).status()).isEqualTo(ProposalStatus.APPROVED);
        verify(proposalService).approve(proposalId);
    }
}
