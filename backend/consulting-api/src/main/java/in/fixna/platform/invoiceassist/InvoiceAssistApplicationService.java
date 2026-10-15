package in.fixna.platform.invoiceassist;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.ai.AiFeature;
import in.fixna.platform.ai.AiProvider;
import in.fixna.platform.ai.AiUsageLog;
import in.fixna.platform.ai.AiUsageLogRepository;
import in.fixna.platform.ai.dto.InvoiceLineIdeasSuggestion;
import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.invoice.Invoice;
import in.fixna.platform.consulting.invoice.InvoiceLine;
import in.fixna.platform.consulting.invoice.InvoiceLineRepository;
import in.fixna.platform.consulting.invoice.InvoiceRepository;
import in.fixna.platform.invoiceassist.dto.InvoiceAssistGenerateRequest;
import in.fixna.platform.invoiceassist.dto.InvoiceSuggestionResponse;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

/**
 * Advisory invoice assistant. Deterministic suggestions are generated first;
 * AI line ideas are optional, advisory-only, and never applied automatically.
 * Invoice state changes go through {@link in.fixna.platform.consulting.invoice.InvoiceStatus}.
 */
@Service
public class InvoiceAssistApplicationService {

    private final InvoiceSuggestionRepository suggestions;
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;
    private final AiProvider aiProvider;
    private final AiUsageLogRepository usageLogs;
    private final AuditPublisher audit;

    public InvoiceAssistApplicationService(
            InvoiceSuggestionRepository suggestions,
            InvoiceRepository invoices,
            InvoiceLineRepository lines,
            AiProvider aiProvider,
            AiUsageLogRepository usageLogs,
            AuditPublisher audit) {
        this.suggestions = suggestions;
        this.invoices = invoices;
        this.lines = lines;
        this.aiProvider = aiProvider;
        this.usageLogs = usageLogs;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<InvoiceSuggestionResponse> list(UUID invoiceId) {
        TenantContext.requirePermission(Permission.INVOICE_ASSIST_USE);
        requireInvoice(invoiceId);
        UUID tenantId = TenantContext.requireTenantId();
        return suggestions.findByInvoiceIdAndTenantIdOrderByCreatedAtDesc(invoiceId, tenantId).stream()
                .map(InvoiceSuggestionResponse::from)
                .toList();
    }

    @Transactional
    public InvoiceSuggestionResponse apply(UUID suggestionId) {
        TenantContext.requirePermission(Permission.INVOICE_ASSIST_USE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceSuggestion suggestion = requireSuggestion(suggestionId);
        if (suggestion.getStatus() != InvoiceSuggestionStatus.SUGGESTED) {
            throw new FixnaException(
                    "SUGGESTION_ALREADY_HANDLED",
                    HttpStatus.CONFLICT,
                    "Suggestion has already been handled");
        }
        if (suggestion.getSuggestionType() != InvoiceSuggestionType.DUE_DATE_RECOMMENDED) {
            throw new FixnaException(
                    "SUGGESTION_NOT_APPLICABLE",
                    HttpStatus.BAD_REQUEST,
                    "Only due date suggestions can be applied automatically");
        }
        Invoice invoice = requireInvoice(suggestion.getInvoiceId());
        if (!invoice.getStatus().isEditable()) {
            throw new FixnaException(
                    "INVOICE_NOT_EDITABLE",
                    HttpStatus.BAD_REQUEST,
                    "Only draft invoices can be edited");
        }
        LocalDate dueDate = LocalDate.parse(suggestion.getPayload().get("dueDate"));
        invoice.setDueDate(dueDate);
        invoices.save(invoice);
        suggestion.setStatus(InvoiceSuggestionStatus.APPLIED);
        suggestions.save(suggestion);
        audit.publish(new AuditEvent(
                "invoice.assist.applied",
                tenantId,
                userId,
                "invoice_suggestion",
                suggestionId.toString(),
                Map.of("invoiceId", invoice.getId().toString(), "dueDate", dueDate.toString()),
                null));
        return InvoiceSuggestionResponse.from(suggestion);
    }

    @Transactional
    public InvoiceSuggestionResponse dismiss(UUID suggestionId) {
        TenantContext.requirePermission(Permission.INVOICE_ASSIST_USE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceSuggestion suggestion = requireSuggestion(suggestionId);
        if (suggestion.getStatus() != InvoiceSuggestionStatus.SUGGESTED) {
            throw new FixnaException(
                    "SUGGESTION_ALREADY_HANDLED",
                    HttpStatus.CONFLICT,
                    "Suggestion has already been handled");
        }
        suggestion.setStatus(InvoiceSuggestionStatus.DISMISSED);
        suggestions.save(suggestion);
        audit.publish(new AuditEvent(
                "invoice.assist.dismissed",
                tenantId,
                userId,
                "invoice_suggestion",
                suggestionId.toString(),
                Map.of("type", suggestion.getSuggestionType().name()),
                null));
        return InvoiceSuggestionResponse.from(suggestion);
    }

    private Optional<InvoiceSuggestion> createIfAbsent(
            UUID tenantId, UUID userId, UUID invoiceId, InvoiceSuggestionType type, Map<String, String> payload) {
        if (isOpenSuggestionExists(invoiceId, tenantId, type)) {
            return Optional.empty();
        }
        return Optional.of(persist(tenantId, userId, invoiceId, type, payload));
    }

    private boolean isOpenSuggestionExists(UUID invoiceId, UUID tenantId, InvoiceSuggestionType type) {
        return suggestions.existsByInvoiceIdAndTenantIdAndSuggestionTypeAndStatus(
                invoiceId, tenantId, type, InvoiceSuggestionStatus.SUGGESTED);
    }

    private InvoiceSuggestion persist(
            UUID tenantId, UUID userId, UUID invoiceId, InvoiceSuggestionType type, Map<String, String> payload) {
        InvoiceSuggestion suggestion = new InvoiceSuggestion();
        suggestion.setId(UUID.randomUUID());
        suggestion.setTenantId(tenantId);
        suggestion.setInvoiceId(invoiceId);
        suggestion.setSuggestionType(type);
        suggestion.setPayload(payload);
        suggestion.setCreatedByUserId(userId);
        return suggestions.save(suggestion);
    }

    private Invoice requireInvoice(UUID invoiceId) {
        UUID tenantId = TenantContext.requireTenantId();
        return invoices.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    private InvoiceSuggestion requireSuggestion(UUID suggestionId) {
        UUID tenantId = TenantContext.requireTenantId();
        return suggestions.findByIdAndTenantId(suggestionId, tenantId)
                .orElseThrow(
                        () -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Suggestion not found"));
    }

    private void logUsage(UUID tenantId, UUID userId, AiFeature feature, String requestSummary) {
        AiUsageLog log = new AiUsageLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setFeature(feature);
        log.setRequestSummary(requestSummary);
        usageLogs.save(log);
    }

    @Transactional
    public List<InvoiceSuggestionResponse> generate(UUID invoiceId, InvoiceAssistGenerateRequest request) {
        TenantContext.requirePermission(Permission.INVOICE_ASSIST_USE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        Invoice invoice = requireInvoice(invoiceId);
        List<InvoiceLine> invoiceLines = lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoiceId, tenantId);
        List<InvoiceSuggestion> created = new ArrayList<>();

        if (invoiceLines.isEmpty()) {
            createIfAbsent(tenantId, userId, invoiceId, InvoiceSuggestionType.MISSING_LINES, Map.of(
                    "hint", "Add at least one line item so the invoice can be reviewed and sent."))
                    .ifPresent(created::add);
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (invoice.getStatus().isEditable() && invoice.getDueDate() == null) {
            createIfAbsent(tenantId, userId, invoiceId, InvoiceSuggestionType.DUE_DATE_RECOMMENDED, Map.of(
                    "dueDate", today.plusDays(14).toString(),
                    "terms", "net_14"))
                    .ifPresent(created::add);
        }

        if (invoice.getDueDate() != null && invoice.getStatus().acceptsPayment() && invoice.getDueDate().isBefore(today)) {
            long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
            createIfAbsent(tenantId, userId, invoiceId, InvoiceSuggestionType.OVERDUE_REMINDER, Map.of(
                    "dueDate", invoice.getDueDate().toString(),
                    "daysOverdue", String.valueOf(daysOverdue),
                    "hint", "Follow up with the client; the invoice is past its due date."))
                    .ifPresent(created::add);
        }

        if (request != null && request.useAiRequested()) {
            created.addAll(generateLineIdeas(tenantId, userId, invoice, invoiceLines));
        }

        if (!created.isEmpty()) {
            audit.publish(new AuditEvent(
                    "invoice.assist.generated",
                    tenantId,
                    userId,
                    "invoice",
                    invoiceId.toString(),
                    Map.of("count", String.valueOf(created.size())),
                    null));
        }
        return created.stream().map(InvoiceSuggestionResponse::from).toList();
    }

    private List<InvoiceSuggestion> generateLineIdeas(
            UUID tenantId, UUID userId, Invoice invoice, List<InvoiceLine> invoiceLines) {
        TenantContext.requirePermission(Permission.AI_USE);
        if (isOpenSuggestionExists(invoice.getId(), tenantId, InvoiceSuggestionType.LINE_IDEAS)) {
            return List.of();
        }
        List<String> descriptions = invoiceLines.stream().map(InvoiceLine::getDescription).toList();
        InvoiceLineIdeasSuggestion ideas = aiProvider.suggestInvoiceLineIdeas(invoice.getTitle(), descriptions);
        if (ideas.ideas().isEmpty()) {
            return List.of();
        }
        InvoiceSuggestion suggestion = persist(
                tenantId, userId, invoice.getId(), InvoiceSuggestionType.LINE_IDEAS, Map.of(
                        "ideas", String.join("\n", ideas.ideas()),
                        "source", "ai_advisory"));
        logUsage(tenantId, userId, AiFeature.INVOICE_LINE_IDEAS, "invoiceId=" + invoice.getId());
        audit.publish(new AuditEvent(
                "ai.suggestion.generated",
                tenantId,
                userId,
                "ai_suggestion",
                suggestion.getId().toString(),
                Map.of("feature", AiFeature.INVOICE_LINE_IDEAS.name()),
                null));
        return List.of(suggestion);
    }
}

