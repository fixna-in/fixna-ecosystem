package in.fixna.platform.consulting.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.billing.ManualPaymentProvider;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.engagement.EngagementRepository;
import in.fixna.platform.consulting.invoice.dto.InvoiceLineRequest;
import in.fixna.platform.consulting.invoice.dto.InvoiceRequest;
import in.fixna.platform.consulting.invoice.dto.InvoiceResponse;
import in.fixna.platform.consulting.invoice.dto.PaymentRequest;
import in.fixna.platform.consulting.invoice.dto.PaymentResponse;
import in.fixna.platform.consulting.proposal.Proposal;
import in.fixna.platform.consulting.proposal.ProposalItem;
import in.fixna.platform.consulting.proposal.ProposalItemRepository;
import in.fixna.platform.consulting.proposal.ProposalRepository;
import in.fixna.platform.consulting.proposal.ProposalStatus;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @Mock InvoiceRepository invoices;
    @Mock InvoiceLineRepository lines;
    @Mock PaymentRepository payments;
    @Mock ClientRepository clients;
    @Mock EngagementRepository engagements;
    @Mock ProposalRepository proposals;
    @Mock ProposalItemRepository proposalItems;
    @Mock DomainEventPublisher events;
    @Mock AuditPublisher audit;

    ManualPaymentProvider paymentProvider = new ManualPaymentProvider();

    InvoiceApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceApplicationService(
                invoices,
                lines,
                payments,
                clients,
                engagements,
                proposals,
                proposalItems,
                paymentProvider,
                events,
                audit);
    }

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
    void createPersistsDraftInvoiceAndPublishesEvent() {
        asConsultantAdmin();
        Client client = new Client();
        client.setId(clientId);
        client.setTenantId(tenantId);
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(invoices.countByTenantId(tenantId)).thenReturn(0L);
        when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(any(), any())).thenReturn(List.of());

        InvoiceResponse response = service.create(
                new InvoiceRequest(clientId, null, null, "Q1 Advisory Invoice", "USD", null));

        assertThat(response.title()).isEqualTo("Q1 Advisory Invoice");
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(response.invoiceNumber()).startsWith("INV-");
        verify(events).publish(any());
        verify(audit).publish(any());
    }

    @Test
    void createFromApprovedProposalCopiesLineItems() {
        asConsultantAdmin();
        UUID proposalId = UUID.randomUUID();
        Client client = new Client();
        client.setId(clientId);
        client.setTenantId(tenantId);
        Proposal proposal = new Proposal();
        proposal.setId(proposalId);
        proposal.setTenantId(tenantId);
        proposal.setClientId(clientId);
        proposal.setStatus(ProposalStatus.APPROVED);
        proposal.setSubtotal(new BigDecimal("100.00"));
        proposal.setTaxAmount(BigDecimal.ZERO);
        proposal.setTotalAmount(new BigDecimal("100.00"));
        proposal.setCurrency("USD");

        ProposalItem item = new ProposalItem();
        item.setDescription("Consulting hours");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setLineTotal(new BigDecimal("100.00"));
        item.setSortOrder(0);

        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(proposals.findByIdAndTenantId(proposalId, tenantId)).thenReturn(Optional.of(proposal));
        when(proposalItems.findByProposalIdAndTenantIdOrderBySortOrderAsc(proposalId, tenantId))
                .thenReturn(List.of(item));
        when(invoices.countByTenantId(tenantId)).thenReturn(0L);
        when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(any(), any()))
                .thenReturn(List.of(new InvoiceLine()));

        InvoiceResponse response = service.create(
                new InvoiceRequest(clientId, null, proposalId, "From proposal", "USD", null));

        assertThat(response.proposalId()).isEqualTo(proposalId);
        assertThat(response.totalAmount()).isEqualByComparingTo("100.00");
        verify(lines).save(any(InvoiceLine.class));
    }

    @Test
    void sendRequiresAtLeastOneLineItem() {
        asConsultantAdmin();
        UUID invoiceId = UUID.randomUUID();
        Invoice existing = draftInvoice(invoiceId);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(existing));
        when(lines.countByInvoiceIdAndTenantId(invoiceId, tenantId)).thenReturn(0L);

        assertThatThrownBy(() -> service.send(invoiceId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVOICE_EMPTY");
    }

    @Test
    void sendMovesDraftToSentWhenLinesExist() {
        asConsultantAdmin();
        UUID invoiceId = UUID.randomUUID();
        Invoice existing = draftInvoice(invoiceId);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(existing));
        when(lines.countByInvoiceIdAndTenantId(invoiceId, tenantId)).thenReturn(1L);
        when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoiceId, tenantId)).thenReturn(List.of());

        InvoiceResponse response = service.send(invoiceId);

        assertThat(response.status()).isEqualTo(InvoiceStatus.SENT);
        assertThat(response.sentAt()).isNotNull();
        verify(events).publish(any());
    }

    @Test
    void partialPaymentMovesSentToPartiallyPaid() {
        asConsultantAdmin();
        UUID invoiceId = UUID.randomUUID();
        Invoice existing = draftInvoice(invoiceId);
        existing.setStatus(InvoiceStatus.SENT);
        existing.setTotalAmount(new BigDecimal("100.00"));
        existing.setAmountPaid(BigDecimal.ZERO);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(existing));
        when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        PaymentResponse response = service.recordPayment(
                invoiceId, new PaymentRequest(new BigDecimal("40.00"), "bank_transfer", "REF-1"));

        assertThat(response.amount()).isEqualByComparingTo("40.00");
        assertThat(existing.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(existing.getAmountPaid()).isEqualByComparingTo("40.00");
        verify(events).publish(any());
    }

    @Test
    void fullPaymentMovesSentToPaid() {
        asConsultantAdmin();
        UUID invoiceId = UUID.randomUUID();
        Invoice existing = draftInvoice(invoiceId);
        existing.setStatus(InvoiceStatus.SENT);
        existing.setTotalAmount(new BigDecimal("100.00"));
        existing.setAmountPaid(BigDecimal.ZERO);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(existing));
        when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        service.recordPayment(invoiceId, new PaymentRequest(new BigDecimal("100.00"), "cash", null));

        assertThat(existing.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(existing.getAmountPaid()).isEqualByComparingTo("100.00");
        verify(events, times(2)).publish(any());
    }

    @Test
    void clientAdminCannotCreateInvoice() {
        asClientAdmin();

        assertThatThrownBy(() -> service.create(
                        new InvoiceRequest(clientId, null, null, "Invoice", "USD", null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void consultantCanManageInvoices() {
        asConsultant();
        Client client = new Client();
        client.setId(clientId);
        client.setTenantId(tenantId);
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(invoices.countByTenantId(tenantId)).thenReturn(0L);
        when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            return invoice;
        });
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(any(), any())).thenReturn(List.of());

        InvoiceResponse response = service.create(
                new InvoiceRequest(clientId, null, null, "Consultant invoice", "USD", null));

        assertThat(response.title()).isEqualTo("Consultant invoice");
    }

    @Test
    void sentInvoiceCannotBeUpdated() {
        asConsultantAdmin();
        UUID invoiceId = UUID.randomUUID();
        Invoice existing = draftInvoice(invoiceId);
        existing.setStatus(InvoiceStatus.SENT);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(
                        invoiceId,
                        new InvoiceRequest(clientId, null, null, "Updated title", "USD", null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVOICE_NOT_EDITABLE");
    }

    @Test
    void draftInvoiceCannotAddLinesWhenNotDraft() {
        asConsultantAdmin();
        UUID invoiceId = UUID.randomUUID();
        Invoice existing = draftInvoice(invoiceId);
        existing.setStatus(InvoiceStatus.SENT);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.addLine(
                        invoiceId,
                        new InvoiceLineRequest("Line item", BigDecimal.ONE, BigDecimal.TEN, 0)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVOICE_NOT_EDITABLE");
    }

    @Test
    void missingInvoiceReturnsNotFound() {
        asConsultantAdmin();
        UUID invoiceId = UUID.randomUUID();
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(invoiceId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }

    private Invoice draftInvoice(UUID invoiceId) {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);
        invoice.setClientId(clientId);
        invoice.setInvoiceNumber("INV-000001");
        invoice.setTitle("Invoice");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCurrency("USD");
        return invoice;
    }
}
