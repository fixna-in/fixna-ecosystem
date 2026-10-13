package in.fixna.platform.invoiceassist;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.ai.AiFeature;
import in.fixna.platform.ai.AiUsageLog;
import in.fixna.platform.ai.AiUsageLogRepository;
import in.fixna.platform.ai.AiProvider;
import in.fixna.platform.ai.dto.InvoiceLineIdeasSuggestion;
import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.invoice.Invoice;
import in.fixna.platform.consulting.invoice.InvoiceLine;
import in.fixna.platform.consulting.invoice.InvoiceLineRepository;
import in.fixna.platform.consulting.invoice.InvoiceRepository;
import in.fixna.platform.consulting.invoice.InvoiceStatus;
import in.fixna.platform.invoiceassist.dto.InvoiceAssistGenerateRequest;
import in.fixna.platform.invoiceassist.dto.InvoiceSuggestionResponse;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceAssistApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID suggestionId = UUID.randomUUID();

    @Mock InvoiceSuggestionRepository suggestions;
    @Mock InvoiceRepository invoices;
    @Mock InvoiceLineRepository lines;
    @Mock AiProvider aiProvider;
    @Mock AiUsageLogRepository usageLogs;
    @Mock AuditPublisher audit;

    @InjectMocks InvoiceAssistApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asConsultant() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT);
    }

    private Invoice invoice(InvoiceStatus status, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);
        invoice.setTitle("Website redesign");
        invoice.setStatus(status);
        invoice.setDueDate(dueDate);
        return invoice;
    }

    private static InvoiceLine line(String description) {
        InvoiceLine line = new InvoiceLine();
        line.setDescription(description);
        return line;
    }

    private InvoiceSuggestion openSuggestion(InvoiceSuggestionType type, Map<String, String> payload) {
        InvoiceSuggestion suggestion = new InvoiceSuggestion();
        suggestion.setId(suggestionId);
        suggestion.setTenantId(tenantId);
        suggestion.setInvoiceId(invoiceId);
        suggestion.setSuggestionType(type);
        suggestion.setPayload(payload);
        suggestion.setStatus(InvoiceSuggestionStatus.SUGGESTED);
        return suggestion;
    }

    @Test
    void generateCreatesDeterministicSuggestionsForDraftInvoice() {
        asConsultant();
        Invoice invoice = invoice(InvoiceStatus.DRAFT, null);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoiceId, tenantId)).thenReturn(List.of());
        when(suggestions.save(any(InvoiceSuggestion.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceSuggestionResponse> response =
                service.generate(invoiceId, new InvoiceAssistGenerateRequest(false));

        assertThat(response).hasSize(2);
        assertThat(response).extracting(InvoiceSuggestionResponse::suggestionType)
                .containsExactlyInAnyOrder("MISSING_LINES", "DUE_DATE_RECOMMENDED");
        assertThat(response).allMatch(r -> "SUGGESTED".equals(r.status()));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).publish(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("invoice.assist.generated");
        assertThat(auditCaptor.getValue().details()).containsEntry("count", "2");
    }

    @Test
    void generateSkipsAlreadyOpenSuggestionTypes() {
        asConsultant();
        Invoice invoice = invoice(InvoiceStatus.DRAFT, null);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoiceId, tenantId)).thenReturn(List.of());
        when(suggestions.existsByInvoiceIdAndTenantIdAndSuggestionTypeAndStatus(
                invoiceId, tenantId, InvoiceSuggestionType.MISSING_LINES, InvoiceSuggestionStatus.SUGGESTED))
                .thenReturn(true);
        when(suggestions.save(any(InvoiceSuggestion.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceSuggestionResponse> response =
                service.generate(invoiceId, new InvoiceAssistGenerateRequest(false));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().suggestionType()).isEqualTo("DUE_DATE_RECOMMENDED");
    }

    @Test
    void generateCreatesOverdueReminderForSentInvoice() {
        asConsultant();
        Invoice invoice = invoice(InvoiceStatus.SENT, LocalDate.now(ZoneOffset.UTC).minusDays(10));
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoiceId, tenantId))
                .thenReturn(List.of(line("Design")));
        when(suggestions.save(any(InvoiceSuggestion.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceSuggestionResponse> response =
                service.generate(invoiceId, new InvoiceAssistGenerateRequest(false));

        assertThat(response).hasSize(1);
        InvoiceSuggestionResponse reminder = response.getFirst();
        assertThat(reminder.suggestionType()).isEqualTo("OVERDUE_REMINDER");
        assertThat(reminder.payload()).containsEntry("daysOverdue", "10");
        assertThat(reminder.payload()).containsKey("dueDate");
    }

    @Test
    void generateCreatesNothingForPaidInvoiceWithLines() {
        asConsultant();
        Invoice invoice = invoice(InvoiceStatus.PAID, LocalDate.now(ZoneOffset.UTC).minusDays(3));
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoiceId, tenantId))
                .thenReturn(List.of(line("Work")));

        List<InvoiceSuggestionResponse> response =
                service.generate(invoiceId, new InvoiceAssistGenerateRequest(false));

        assertThat(response).isEmpty();
        verify(audit, never()).publish(any());
        verify(suggestions, never()).save(any(InvoiceSuggestion.class));
    }

    @Test
    void applyDueDateSuggestionUpdatesDraftInvoice() {
        asConsultant();
        InvoiceSuggestion suggestion = openSuggestion(
                InvoiceSuggestionType.DUE_DATE_RECOMMENDED, Map.of("dueDate", "2026-10-20"));
        when(suggestions.findByIdAndTenantId(suggestionId, tenantId)).thenReturn(Optional.of(suggestion));
        Invoice invoice = invoice(InvoiceStatus.DRAFT, null);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(invoices.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(suggestions.save(any(InvoiceSuggestion.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceSuggestionResponse response = service.apply(suggestionId);

        assertThat(response.status()).isEqualTo("APPLIED");
        assertThat(invoice.getDueDate()).isEqualTo(LocalDate.parse("2026-10-20"));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).publish(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("invoice.assist.applied");
    }

    @Test
    void applyRejectsNonDueDateSuggestions() {
        asConsultant();
        when(suggestions.findByIdAndTenantId(suggestionId, tenantId))
                .thenReturn(Optional.of(openSuggestion(InvoiceSuggestionType.MISSING_LINES, Map.of("hint", "x"))));

        assertThatThrownBy(() -> service.apply(suggestionId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("SUGGESTION_NOT_APPLICABLE");
    }

    @Test
    void applyRejectsSentInvoices() {
        asConsultant();
        when(suggestions.findByIdAndTenantId(suggestionId, tenantId))
                .thenReturn(Optional.of(openSuggestion(
                        InvoiceSuggestionType.DUE_DATE_RECOMMENDED, Map.of("dueDate", "2026-10-20"))));
        when(invoices.findByIdAndTenantId(invoiceId, tenantId))
                .thenReturn(Optional.of(invoice(InvoiceStatus.SENT, null)));

        assertThatThrownBy(() -> service.apply(suggestionId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVOICE_NOT_EDITABLE");
    }

    @Test
    void dismissMarksSuggestionDismissed() {
        asConsultant();
        InvoiceSuggestion suggestion = openSuggestion(
                InvoiceSuggestionType.DUE_DATE_RECOMMENDED, Map.of("dueDate", "2026-10-20"));
        when(suggestions.findByIdAndTenantId(suggestionId, tenantId)).thenReturn(Optional.of(suggestion));
        when(suggestions.save(any(InvoiceSuggestion.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceSuggestionResponse response = service.dismiss(suggestionId);

        assertThat(response.status()).isEqualTo("DISMISSED");
        verify(audit).publish(any(AuditEvent.class));
    }

    @Test
    void generateWithAiAddsAdvisoryLineIdeas() {
        asConsultant();
        Invoice invoice = invoice(InvoiceStatus.DRAFT, LocalDate.now(ZoneOffset.UTC).plusDays(5));
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoiceId, tenantId))
                .thenReturn(List.of(line("Design")));
        when(aiProvider.suggestInvoiceLineIdeas("Website redesign", List.of("Design")))
                .thenReturn(new InvoiceLineIdeasSuggestion(List.of("Build and launch")));
        when(suggestions.save(any(InvoiceSuggestion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usageLogs.save(any(AiUsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        List<InvoiceSuggestionResponse> response =
                service.generate(invoiceId, new InvoiceAssistGenerateRequest(true));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().suggestionType()).isEqualTo("LINE_IDEAS");
        assertThat(response.getFirst().payload()).containsEntry("source", "ai_advisory");

        ArgumentCaptor<AiUsageLog> logCaptor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(usageLogs).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getFeature()).isEqualTo(AiFeature.INVOICE_LINE_IDEAS);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(AuditEvent::action)
                .containsExactlyInAnyOrder("invoice.assist.generated", "ai.suggestion.generated");
    }

    @Test
    void clientUserCannotUseInvoiceAssist() {
        TenantContext.set(tenantId, userId, Role.CLIENT_USER);

        assertThatThrownBy(() -> service.generate(invoiceId, new InvoiceAssistGenerateRequest(false)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> service.list(invoiceId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listReturnsInvoiceSuggestions() {
        asConsultant();
        Invoice invoice = invoice(InvoiceStatus.DRAFT, null);
        when(invoices.findByIdAndTenantId(invoiceId, tenantId)).thenReturn(Optional.of(invoice));
        when(suggestions.findByInvoiceIdAndTenantIdOrderByCreatedAtDesc(invoiceId, tenantId))
                .thenReturn(List.of(openSuggestion(InvoiceSuggestionType.MISSING_LINES, Map.of("hint", "x"))));

        List<InvoiceSuggestionResponse> response = service.list(invoiceId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().suggestionType()).isEqualTo("MISSING_LINES");
    }
}