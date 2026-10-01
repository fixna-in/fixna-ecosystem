package in.fixna.platform.consulting.portal;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.invoice.Invoice;
import in.fixna.platform.consulting.invoice.InvoiceLine;
import in.fixna.platform.consulting.invoice.InvoiceLineRepository;
import in.fixna.platform.consulting.invoice.InvoiceRepository;
import in.fixna.platform.consulting.invoice.InvoiceStatus;
import in.fixna.platform.consulting.invoice.PaymentRepository;
import in.fixna.platform.consulting.invoice.dto.InvoiceResponse;
import in.fixna.platform.consulting.invoice.dto.PaymentResponse;
import in.fixna.platform.consulting.meeting.MeetingRepository;
import in.fixna.platform.consulting.meeting.dto.MeetingResponse;
import in.fixna.platform.consulting.portal.dto.PortalInvoiceDetailResponse;
import in.fixna.platform.consulting.portal.dto.PortalMeResponse;
import in.fixna.platform.consulting.project.ProjectRepository;
import in.fixna.platform.consulting.project.dto.ProjectResponse;
import in.fixna.platform.consulting.proposal.Proposal;
import in.fixna.platform.consulting.proposal.ProposalItem;
import in.fixna.platform.consulting.proposal.ProposalItemRepository;
import in.fixna.platform.consulting.proposal.ProposalRepository;
import in.fixna.platform.consulting.proposal.ProposalStatus;
import in.fixna.platform.consulting.proposal.ProposalApplicationService;
import in.fixna.platform.consulting.proposal.dto.ProposalResponse;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class PortalApplicationService {

    private static final List<ProposalStatus> HIDDEN_PROPOSAL_STATUSES =
            List.of(ProposalStatus.DRAFT, ProposalStatus.CANCELLED);

    private final ClientRepository clients;
    private final ProjectRepository projects;
    private final ProposalRepository proposals;
    private final ProposalItemRepository proposalItems;
    private final ProposalApplicationService proposalService;
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository invoiceLines;
    private final PaymentRepository payments;
    private final MeetingRepository meetings;

    public PortalApplicationService(
            ClientRepository clients,
            ProjectRepository projects,
            ProposalRepository proposals,
            ProposalItemRepository proposalItems,
            ProposalApplicationService proposalService,
            InvoiceRepository invoices,
            InvoiceLineRepository invoiceLines,
            PaymentRepository payments,
            MeetingRepository meetings) {
        this.clients = clients;
        this.projects = projects;
        this.proposals = proposals;
        this.proposalItems = proposalItems;
        this.proposalService = proposalService;
        this.invoices = invoices;
        this.invoiceLines = invoiceLines;
        this.payments = payments;
        this.meetings = meetings;
    }

    @Transactional(readOnly = true)
    public PortalMeResponse me() {
        UUID clientId = TenantContext.requireClientId();
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        Client client = requireClient(clientId, tenantId);
        return new PortalMeResponse(
                userId, tenantId, clientId, client.getName(), TenantContext.currentRole());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        TenantContext.requirePermission(Permission.PROJECT_VIEW);
        UUID clientId = TenantContext.requireClientId();
        UUID tenantId = TenantContext.requireTenantId();
        return projects.findByClientIdAndTenantIdOrderByCreatedAtDesc(clientId, tenantId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> listProposals() {
        TenantContext.requirePermission(Permission.PROPOSAL_VIEW);
        UUID clientId = TenantContext.requireClientId();
        UUID tenantId = TenantContext.requireTenantId();
        return proposals
                .findByClientIdAndTenantIdAndStatusNotInOrderByCreatedAtDesc(
                        clientId, tenantId, HIDDEN_PROPOSAL_STATUSES)
                .stream()
                .map(ProposalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProposalResponse getProposal(UUID id) {
        TenantContext.requirePermission(Permission.PROPOSAL_VIEW);
        Proposal proposal = requireVisibleProposal(id);
        return toProposalResponse(proposal);
    }

    @Transactional
    public ProposalResponse approveProposal(UUID id) {
        TenantContext.requirePermission(Permission.PROPOSAL_APPROVE);
        requireVisibleProposal(id);
        return proposalService.approve(id);
    }

    @Transactional
    public ProposalResponse rejectProposal(UUID id) {
        TenantContext.requirePermission(Permission.PROPOSAL_APPROVE);
        requireVisibleProposal(id);
        return proposalService.reject(id);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices() {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        UUID clientId = TenantContext.requireClientId();
        UUID tenantId = TenantContext.requireTenantId();
        return invoices
                .findByClientIdAndTenantIdAndStatusNotOrderByCreatedAtDesc(
                        clientId, tenantId, InvoiceStatus.DRAFT)
                .stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PortalInvoiceDetailResponse getInvoice(UUID id) {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        UUID clientId = TenantContext.requireClientId();
        UUID tenantId = TenantContext.requireTenantId();
        Invoice invoice = invoices
                .findByIdAndTenantIdAndClientId(id, tenantId, clientId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Invoice not found");
        }
        List<InvoiceLine> lines =
                invoiceLines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoice.getId(), tenantId);
        List<PaymentResponse> paymentResponses = payments
                .findByInvoiceIdAndTenantIdOrderByPaidAtDesc(invoice.getId(), tenantId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
        return new PortalInvoiceDetailResponse(InvoiceResponse.from(invoice, lines), paymentResponses);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> listMeetings() {
        TenantContext.requirePermission(Permission.MEETING_VIEW);
        UUID clientId = TenantContext.requireClientId();
        UUID tenantId = TenantContext.requireTenantId();
        return meetings.findByTenantIdAndClientIdOrderByStartsAtAsc(tenantId, clientId).stream()
                .map(MeetingResponse::from)
                .toList();
    }

    private Proposal requireVisibleProposal(UUID id) {
        UUID clientId = TenantContext.requireClientId();
        UUID tenantId = TenantContext.requireTenantId();
        Proposal proposal = proposals
                .findByIdAndTenantIdAndClientId(id, tenantId, clientId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Proposal not found"));
        if (HIDDEN_PROPOSAL_STATUSES.contains(proposal.getStatus())) {
            throw new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Proposal not found");
        }
        return proposal;
    }

    private ProposalResponse toProposalResponse(Proposal proposal) {
        List<ProposalItem> items = proposalItems.findByProposalIdAndTenantIdOrderBySortOrderAsc(
                proposal.getId(), proposal.getTenantId());
        return ProposalResponse.from(proposal, items);
    }

    private Client requireClient(UUID clientId, UUID tenantId) {
        return clients.findByIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Client not found"));
    }
}
