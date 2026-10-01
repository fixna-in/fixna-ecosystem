package in.fixna.platform.consulting.invoice;

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
import in.fixna.platform.billing.PaymentProvider;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.engagement.Engagement;
import in.fixna.platform.consulting.engagement.EngagementRepository;
import in.fixna.platform.consulting.invoice.dto.InvoiceLineRequest;
import in.fixna.platform.consulting.invoice.dto.InvoiceLineResponse;
import in.fixna.platform.consulting.invoice.dto.InvoiceRequest;
import in.fixna.platform.consulting.invoice.dto.InvoiceResponse;
import in.fixna.platform.consulting.invoice.dto.PaymentRequest;
import in.fixna.platform.consulting.invoice.dto.PaymentResponse;
import in.fixna.platform.consulting.invoice.event.InvoiceCreated;
import in.fixna.platform.consulting.invoice.event.InvoicePaid;
import in.fixna.platform.consulting.invoice.event.InvoiceSent;
import in.fixna.platform.consulting.invoice.event.PaymentRecorded;
import in.fixna.platform.consulting.proposal.Proposal;
import in.fixna.platform.consulting.proposal.ProposalItem;
import in.fixna.platform.consulting.proposal.ProposalItemRepository;
import in.fixna.platform.consulting.proposal.ProposalRepository;
import in.fixna.platform.consulting.proposal.ProposalStatus;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class InvoiceApplicationService {

    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;
    private final PaymentRepository payments;
    private final ClientRepository clients;
    private final EngagementRepository engagements;
    private final ProposalRepository proposals;
    private final ProposalItemRepository proposalItems;
    private final PaymentProvider paymentProvider;
    private final DomainEventPublisher events;
    private final AuditPublisher audit;

    public InvoiceApplicationService(
            InvoiceRepository invoices,
            InvoiceLineRepository lines,
            PaymentRepository payments,
            ClientRepository clients,
            EngagementRepository engagements,
            ProposalRepository proposals,
            ProposalItemRepository proposalItems,
            PaymentProvider paymentProvider,
            DomainEventPublisher events,
            AuditPublisher audit) {
        this.invoices = invoices;
        this.lines = lines;
        this.payments = payments;
        this.clients = clients;
        this.engagements = engagements;
        this.proposals = proposals;
        this.proposalItems = proposalItems;
        this.paymentProvider = paymentProvider;
        this.events = events;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listByClient(UUID clientId) {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        requireClient(clientId);
        UUID tenantId = TenantContext.requireTenantId();
        return invoices.findByClientIdAndTenantIdOrderByCreatedAtDesc(clientId, tenantId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        requireClient(request.clientId());
        requireEngagementForClient(request.engagementId(), request.clientId());

        Invoice invoice = new Invoice();
        invoice.setTenantId(tenantId);
        invoice.setClientId(request.clientId());
        invoice.setInvoiceNumber(nextInvoiceNumber(tenantId));
        applyFields(invoice, request);
        invoices.save(invoice);

        if (request.proposalId() != null) {
            copyFromApprovedProposal(invoice, request.proposalId(), request.clientId());
            invoices.save(invoice);
        }

        events.publish(new InvoiceCreated(invoice.getId(), tenantId));
        audit.publish(new AuditEvent(
                "invoice.created",
                tenantId,
                TenantContext.requireUserId(),
                "invoice",
                invoice.getId().toString(),
                Map.of("title", invoice.getTitle(), "clientId", request.clientId().toString()),
                null));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID id) {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        Invoice invoice = requireInvoice(id);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse update(UUID id, InvoiceRequest request) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        Invoice invoice = requireInvoice(id);
        requireEditable(invoice);
        if (!invoice.getClientId().equals(request.clientId())) {
            requireClient(request.clientId());
        }
        requireEngagementForClient(request.engagementId(), request.clientId());
        invoice.setClientId(request.clientId());
        applyFields(invoice, request);
        invoices.save(invoice);

        audit.publish(new AuditEvent(
                "invoice.updated",
                invoice.getTenantId(),
                TenantContext.requireUserId(),
                "invoice",
                invoice.getId().toString(),
                Map.of("title", invoice.getTitle()),
                null));
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceLineResponse addLine(UUID invoiceId, InvoiceLineRequest request) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        Invoice invoice = requireInvoice(invoiceId);
        requireEditable(invoice);

        InvoiceLine line = new InvoiceLine();
        line.setTenantId(invoice.getTenantId());
        line.setInvoiceId(invoice.getId());
        applyLineFields(line, request);
        lines.save(line);
        recalculateTotals(invoice);

        audit.publish(new AuditEvent(
                "invoice.line_added",
                invoice.getTenantId(),
                TenantContext.requireUserId(),
                "invoice",
                invoice.getId().toString(),
                Map.of("lineId", line.getId().toString()),
                null));
        return InvoiceLineResponse.from(line);
    }

    @Transactional
    public InvoiceResponse deleteLine(UUID invoiceId, UUID lineId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        Invoice invoice = requireInvoice(invoiceId);
        requireEditable(invoice);
        requireLine(invoiceId, lineId);
        lines.deleteByIdAndInvoiceIdAndTenantId(lineId, invoiceId, invoice.getTenantId());
        recalculateTotals(invoice);

        audit.publish(new AuditEvent(
                "invoice.line_deleted",
                invoice.getTenantId(),
                TenantContext.requireUserId(),
                "invoice",
                invoice.getId().toString(),
                Map.of("lineId", lineId.toString()),
                null));
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse send(UUID id) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        Invoice invoice = requireInvoice(id);
        InvoiceStatus fromStatus = invoice.getStatus();
        fromStatus.validateTransitionTo(InvoiceStatus.SENT);

        long lineCount = lines.countByInvoiceIdAndTenantId(id, invoice.getTenantId());
        if (lineCount < 1) {
            throw new FixnaException(
                    "INVOICE_EMPTY", HttpStatus.BAD_REQUEST, "Invoice must have at least one line item to send");
        }

        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(OffsetDateTime.now());
        invoices.save(invoice);

        events.publish(new InvoiceSent(invoice.getId(), invoice.getTenantId()));
        audit.publish(new AuditEvent(
                "invoice.sent",
                invoice.getTenantId(),
                TenantContext.requireUserId(),
                "invoice",
                invoice.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", InvoiceStatus.SENT.name()),
                null));
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(UUID id) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        Invoice invoice = requireInvoice(id);
        InvoiceStatus fromStatus = invoice.getStatus();
        fromStatus.validateTransitionTo(InvoiceStatus.CANCELLED);
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoices.save(invoice);

        audit.publish(new AuditEvent(
                "invoice.cancelled",
                invoice.getTenantId(),
                TenantContext.requireUserId(),
                "invoice",
                invoice.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", InvoiceStatus.CANCELLED.name()),
                null));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(UUID invoiceId) {
        TenantContext.requirePermission(Permission.PAYMENT_VIEW);
        Invoice invoice = requireInvoice(invoiceId);
        return payments.findByInvoiceIdAndTenantIdOrderByPaidAtDesc(invoiceId, invoice.getTenantId()).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse recordPayment(UUID invoiceId, PaymentRequest request) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        Invoice invoice = requireInvoice(invoiceId);
        if (!invoice.getStatus().acceptsPayment()) {
            throw new FixnaException(
                    "INVOICE_NOT_PAYABLE",
                    HttpStatus.BAD_REQUEST,
                    "Payments can only be recorded for sent or partially paid invoices");
        }

        BigDecimal remaining = invoice.remainingBalance();
        paymentProvider.validatePaymentAmount(request.amount(), remaining);

        Payment payment = new Payment();
        payment.setTenantId(invoice.getTenantId());
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(request.amount());
        payment.setCurrency(invoice.getCurrency());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setReference(request.reference());
        payment.setRecordedByUserId(TenantContext.requireUserId());
        payments.save(payment);

        InvoiceStatus fromStatus = invoice.getStatus();
        BigDecimal newAmountPaid = invoice.getAmountPaid().add(request.amount());
        invoice.setAmountPaid(newAmountPaid);

        InvoiceStatus toStatus;
        if (newAmountPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            toStatus = InvoiceStatus.PAID;
        } else {
            toStatus = InvoiceStatus.PARTIALLY_PAID;
        }
        fromStatus.validateTransitionTo(toStatus);
        invoice.setStatus(toStatus);
        invoices.save(invoice);

        events.publish(new PaymentRecorded(
                payment.getId(), invoice.getId(), invoice.getTenantId(), payment.getAmount()));
        if (toStatus == InvoiceStatus.PAID) {
            events.publish(new InvoicePaid(invoice.getId(), invoice.getTenantId()));
        }

        audit.publish(new AuditEvent(
                "payment.recorded",
                invoice.getTenantId(),
                TenantContext.requireUserId(),
                "invoice",
                invoice.getId().toString(),
                Map.of(
                        "paymentId",
                        payment.getId().toString(),
                        "amount",
                        request.amount().toPlainString(),
                        "fromStatus",
                        fromStatus.name(),
                        "toStatus",
                        toStatus.name()),
                null));
        return PaymentResponse.from(payment);
    }

    private void copyFromApprovedProposal(Invoice invoice, UUID proposalId, UUID clientId) {
        UUID tenantId = invoice.getTenantId();
        Proposal proposal = proposals.findByIdAndTenantId(proposalId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Proposal not found"));
        if (proposal.getStatus() != ProposalStatus.APPROVED) {
            throw new FixnaException(
                    "PROPOSAL_NOT_APPROVED",
                    HttpStatus.BAD_REQUEST,
                    "Invoice can only be created from an approved proposal");
        }
        if (!proposal.getClientId().equals(clientId)) {
            throw new FixnaException(
                    "INVALID_PROPOSAL",
                    HttpStatus.BAD_REQUEST,
                    "Proposal does not belong to the specified client");
        }

        invoice.setProposalId(proposal.getId());
        invoice.setEngagementId(proposal.getEngagementId());
        invoice.setCurrency(proposal.getCurrency());
        invoice.setSubtotal(proposal.getSubtotal());
        invoice.setTaxAmount(proposal.getTaxAmount());
        invoice.setTotalAmount(proposal.getTotalAmount());

        List<ProposalItem> items =
                proposalItems.findByProposalIdAndTenantIdOrderBySortOrderAsc(proposalId, tenantId);
        for (ProposalItem item : items) {
            InvoiceLine line = new InvoiceLine();
            line.setTenantId(tenantId);
            line.setInvoiceId(invoice.getId());
            line.setDescription(item.getDescription());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getUnitPrice());
            line.setLineTotal(item.getLineTotal());
            line.setSortOrder(item.getSortOrder());
            lines.save(line);
        }
    }

    private String nextInvoiceNumber(UUID tenantId) {
        long count = invoices.countByTenantId(tenantId);
        return "INV-" + String.format("%06d", count + 1);
    }

    private Invoice requireInvoice(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return invoices.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    private InvoiceLine requireLine(UUID invoiceId, UUID lineId) {
        UUID tenantId = TenantContext.requireTenantId();
        return lines.findByIdAndInvoiceIdAndTenantId(lineId, invoiceId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Invoice line not found"));
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

    private static void requireEditable(Invoice invoice) {
        if (!invoice.getStatus().isEditable()) {
            throw new FixnaException(
                    "INVOICE_NOT_EDITABLE",
                    HttpStatus.BAD_REQUEST,
                    "Only draft invoices can be edited");
        }
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLine> invoiceLines =
                lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoice.getId(), invoice.getTenantId());
        return InvoiceResponse.from(invoice, invoiceLines);
    }

    private void recalculateTotals(Invoice invoice) {
        List<InvoiceLine> invoiceLines =
                lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoice.getId(), invoice.getTenantId());
        BigDecimal subtotal = invoiceLines.stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = BigDecimal.ZERO;
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(subtotal.add(taxAmount));
        invoices.save(invoice);
    }

    private static void applyFields(Invoice invoice, InvoiceRequest request) {
        invoice.setEngagementId(request.engagementId());
        invoice.setTitle(request.title());
        if (request.currency() != null) {
            invoice.setCurrency(request.currency());
        }
        invoice.setDueDate(request.dueDate());
    }

    private static void applyLineFields(InvoiceLine line, InvoiceLineRequest request) {
        line.setDescription(request.description());
        line.setQuantity(request.quantity());
        line.setUnitPrice(request.unitPrice());
        line.setLineTotal(request.quantity().multiply(request.unitPrice()).setScale(2, RoundingMode.HALF_UP));
        line.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
    }
}
