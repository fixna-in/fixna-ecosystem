package in.fixna.platform.consulting.proposal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.engagement.Engagement;
import in.fixna.platform.consulting.engagement.EngagementRepository;
import in.fixna.platform.consulting.proposal.dto.ProposalItemRequest;
import in.fixna.platform.consulting.proposal.dto.ProposalItemResponse;
import in.fixna.platform.consulting.proposal.dto.ProposalRequest;
import in.fixna.platform.consulting.proposal.dto.ProposalResponse;
import in.fixna.platform.consulting.proposal.event.ProposalApproved;
import in.fixna.platform.consulting.proposal.event.ProposalCreated;
import in.fixna.platform.consulting.proposal.event.ProposalRejected;
import in.fixna.platform.consulting.proposal.event.ProposalSent;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class ProposalApplicationService {

    private final ProposalRepository proposals;
    private final ProposalItemRepository items;
    private final ClientRepository clients;
    private final EngagementRepository engagements;
    private final DomainEventPublisher events;
    private final AuditPublisher audit;

    public ProposalApplicationService(
            ProposalRepository proposals,
            ProposalItemRepository items,
            ClientRepository clients,
            EngagementRepository engagements,
            DomainEventPublisher events,
            AuditPublisher audit) {
        this.proposals = proposals;
        this.items = items;
        this.clients = clients;
        this.engagements = engagements;
        this.events = events;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> listByClient(UUID clientId) {
        TenantContext.requirePermission(Permission.PROPOSAL_VIEW);
        requireClient(clientId);
        UUID tenantId = TenantContext.requireTenantId();
        return proposals.findByClientIdAndTenantIdOrderByCreatedAtDesc(clientId, tenantId).stream()
                .map(ProposalResponse::from)
                .toList();
    }

    @Transactional
    public ProposalResponse create(ProposalRequest request) {
        TenantContext.requirePermission(Permission.PROPOSAL_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        requireClient(request.clientId());
        requireEngagementForClient(request.engagementId(), request.clientId());

        Proposal proposal = new Proposal();
        proposal.setTenantId(tenantId);
        proposal.setClientId(request.clientId());
        applyFields(proposal, request);
        proposals.save(proposal);

        events.publish(new ProposalCreated(proposal.getId(), tenantId));
        audit.publish(new AuditEvent(
                "proposal.created",
                tenantId,
                TenantContext.requireUserId(),
                "proposal",
                proposal.getId().toString(),
                Map.of("title", proposal.getTitle(), "clientId", request.clientId().toString()),
                null));
        return ProposalResponse.from(proposal);
    }

    @Transactional(readOnly = true)
    public ProposalResponse get(UUID id) {
        TenantContext.requirePermission(Permission.PROPOSAL_VIEW);
        Proposal proposal = requireProposal(id);
        return toResponse(proposal);
    }

    @Transactional
    public ProposalResponse update(UUID id, ProposalRequest request) {
        TenantContext.requirePermission(Permission.PROPOSAL_MANAGE);
        Proposal proposal = requireProposal(id);
        requireEditable(proposal);
        if (!proposal.getClientId().equals(request.clientId())) {
            requireClient(request.clientId());
        }
        requireEngagementForClient(request.engagementId(), request.clientId());
        proposal.setClientId(request.clientId());
        applyFields(proposal, request);
        proposals.save(proposal);

        audit.publish(new AuditEvent(
                "proposal.updated",
                proposal.getTenantId(),
                TenantContext.requireUserId(),
                "proposal",
                proposal.getId().toString(),
                Map.of("title", proposal.getTitle()),
                null));
        return toResponse(proposal);
    }

    @Transactional
    public ProposalItemResponse addItem(UUID proposalId, ProposalItemRequest request) {
        TenantContext.requirePermission(Permission.PROPOSAL_MANAGE);
        Proposal proposal = requireProposal(proposalId);
        requireEditable(proposal);

        ProposalItem item = new ProposalItem();
        item.setTenantId(proposal.getTenantId());
        item.setProposalId(proposal.getId());
        applyItemFields(item, request);
        items.save(item);
        recalculateTotals(proposal);

        audit.publish(new AuditEvent(
                "proposal.item_added",
                proposal.getTenantId(),
                TenantContext.requireUserId(),
                "proposal",
                proposal.getId().toString(),
                Map.of("itemId", item.getId().toString()),
                null));
        return ProposalItemResponse.from(item);
    }

    @Transactional
    public ProposalItemResponse updateItem(UUID proposalId, UUID itemId, ProposalItemRequest request) {
        TenantContext.requirePermission(Permission.PROPOSAL_MANAGE);
        Proposal proposal = requireProposal(proposalId);
        requireEditable(proposal);
        ProposalItem item = requireItem(proposalId, itemId);
        applyItemFields(item, request);
        items.save(item);
        recalculateTotals(proposal);

        audit.publish(new AuditEvent(
                "proposal.item_updated",
                proposal.getTenantId(),
                TenantContext.requireUserId(),
                "proposal",
                proposal.getId().toString(),
                Map.of("itemId", itemId.toString()),
                null));
        return ProposalItemResponse.from(item);
    }

    @Transactional
    public ProposalResponse deleteItem(UUID proposalId, UUID itemId) {
        TenantContext.requirePermission(Permission.PROPOSAL_MANAGE);
        Proposal proposal = requireProposal(proposalId);
        requireEditable(proposal);
        requireItem(proposalId, itemId);
        items.deleteByIdAndProposalIdAndTenantId(itemId, proposalId, proposal.getTenantId());
        recalculateTotals(proposal);

        audit.publish(new AuditEvent(
                "proposal.item_deleted",
                proposal.getTenantId(),
                TenantContext.requireUserId(),
                "proposal",
                proposal.getId().toString(),
                Map.of("itemId", itemId.toString()),
                null));
        return toResponse(proposal);
    }

    @Transactional
    public ProposalResponse send(UUID id) {
        TenantContext.requirePermission(Permission.PROPOSAL_MANAGE);
        Proposal proposal = requireProposal(id);
        ProposalStatus fromStatus = proposal.getStatus();
        fromStatus.validateTransitionTo(ProposalStatus.SENT);

        long itemCount = items.countByProposalIdAndTenantId(id, proposal.getTenantId());
        if (itemCount < 1) {
            throw new FixnaException(
                    "PROPOSAL_EMPTY", HttpStatus.BAD_REQUEST, "Proposal must have at least one line item to send");
        }

        proposal.setStatus(ProposalStatus.SENT);
        proposal.setSentAt(OffsetDateTime.now());
        proposals.save(proposal);

        events.publish(new ProposalSent(proposal.getId(), proposal.getTenantId()));
        audit.publish(new AuditEvent(
                "proposal.sent",
                proposal.getTenantId(),
                TenantContext.requireUserId(),
                "proposal",
                proposal.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", ProposalStatus.SENT.name()),
                null));
        return toResponse(proposal);
    }

    @Transactional
    public ProposalResponse approve(UUID id) {
        TenantContext.requirePermission(Permission.PROPOSAL_APPROVE);
        return transitionStatus(id, ProposalStatus.APPROVED, "proposal.approved");
    }

    @Transactional
    public ProposalResponse reject(UUID id) {
        TenantContext.requirePermission(Permission.PROPOSAL_APPROVE);
        return transitionStatus(id, ProposalStatus.REJECTED, "proposal.rejected");
    }

    @Transactional
    public ProposalResponse cancel(UUID id) {
        TenantContext.requirePermission(Permission.PROPOSAL_MANAGE);
        Proposal proposal = requireProposal(id);
        ProposalStatus fromStatus = proposal.getStatus();
        fromStatus.validateTransitionTo(ProposalStatus.CANCELLED);
        proposal.setStatus(ProposalStatus.CANCELLED);
        proposals.save(proposal);

        audit.publish(new AuditEvent(
                "proposal.cancelled",
                proposal.getTenantId(),
                TenantContext.requireUserId(),
                "proposal",
                proposal.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", ProposalStatus.CANCELLED.name()),
                null));
        return toResponse(proposal);
    }

    private ProposalResponse transitionStatus(UUID id, ProposalStatus toStatus, String auditAction) {
        Proposal proposal = requireProposal(id);
        ProposalStatus fromStatus = proposal.getStatus();
        fromStatus.validateTransitionTo(toStatus);
        proposal.setStatus(toStatus);
        proposals.save(proposal);

        if (toStatus == ProposalStatus.APPROVED) {
            events.publish(new ProposalApproved(proposal.getId(), proposal.getTenantId()));
        } else if (toStatus == ProposalStatus.REJECTED) {
            events.publish(new ProposalRejected(proposal.getId(), proposal.getTenantId()));
        }
        audit.publish(new AuditEvent(
                auditAction,
                proposal.getTenantId(),
                TenantContext.requireUserId(),
                "proposal",
                proposal.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", toStatus.name()),
                null));
        return toResponse(proposal);
    }

    private Proposal requireProposal(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return proposals.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Proposal not found"));
    }

    private ProposalItem requireItem(UUID proposalId, UUID itemId) {
        UUID tenantId = TenantContext.requireTenantId();
        return items.findByIdAndProposalIdAndTenantId(itemId, proposalId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Proposal item not found"));
    }

    private void requireClient(UUID clientId) {
        UUID tenantId = TenantContext.requireTenantId();
        clients.findByIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Client not found"));
    }

    private void requireEngagementForClient(UUID engagementId, UUID clientId) {
        if (engagementId == null) {
            return;
        }
        UUID tenantId = TenantContext.requireTenantId();
        Engagement engagement = engagements.findByIdAndTenantId(engagementId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Engagement not found"));
        if (!engagement.getClientId().equals(clientId)) {
            throw new FixnaException(
                    "INVALID_ENGAGEMENT",
                    HttpStatus.BAD_REQUEST,
                    "Engagement does not belong to the specified client");
        }
    }

    private static void requireEditable(Proposal proposal) {
        if (!proposal.getStatus().isEditable()) {
            throw new FixnaException(
                    "PROPOSAL_NOT_EDITABLE",
                    HttpStatus.BAD_REQUEST,
                    "Only draft proposals can be edited");
        }
    }

    private ProposalResponse toResponse(Proposal proposal) {
        List<ProposalItem> proposalItems =
                items.findByProposalIdAndTenantIdOrderBySortOrderAsc(proposal.getId(), proposal.getTenantId());
        return ProposalResponse.from(proposal, proposalItems);
    }

    private void recalculateTotals(Proposal proposal) {
        List<ProposalItem> proposalItems =
                items.findByProposalIdAndTenantIdOrderBySortOrderAsc(proposal.getId(), proposal.getTenantId());
        BigDecimal subtotal = proposalItems.stream()
                .map(ProposalItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = BigDecimal.ZERO;
        proposal.setSubtotal(subtotal);
        proposal.setTaxAmount(taxAmount);
        proposal.setTotalAmount(subtotal.add(taxAmount));
        proposals.save(proposal);
    }

    private static void applyFields(Proposal proposal, ProposalRequest request) {
        proposal.setEngagementId(request.engagementId());
        proposal.setTitle(request.title());
        proposal.setDescription(request.description());
        if (request.currency() != null) {
            proposal.setCurrency(request.currency());
        }
        proposal.setValidUntil(request.validUntil());
    }

    private static void applyItemFields(ProposalItem item, ProposalItemRequest request) {
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setLineTotal(request.quantity().multiply(request.unitPrice()).setScale(2, RoundingMode.HALF_UP));
        item.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
    }
}
