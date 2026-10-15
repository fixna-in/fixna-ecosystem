package in.fixna.platform.invoiceassist;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import in.fixna.platform.consulting.invoice.InvoiceStatus;
import in.fixna.platform.file.FileStorage;
import in.fixna.platform.invoiceassist.dto.InvoiceDocumentJobResponse;
import in.fixna.platform.invoiceassist.dto.InvoiceDocumentResponse;
import in.fixna.platform.invoiceassist.dto.InvoiceExtractionResponse;
import in.fixna.platform.invoiceassist.dto.InvoiceExtractionSummary;
import in.fixna.platform.invoiceassist.dto.InvoiceFieldConfirmRequest;
import in.fixna.platform.invoiceassist.dto.InvoiceFieldResponse;
import in.fixna.platform.invoiceassist.MalwareScanner;
import in.fixna.platform.invoiceassist.ScanResult;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;
import in.fixna.platform.invoiceassist.FieldExtractor.Draft;
import in.fixna.platform.invoiceassist.PdfTextExtractor.PageText;

/**
 * Private invoice document intake (ADR-011) with provenance-backed
 * extraction (ADR-012). Upload is PDF-only and size-capped; bytes never leave
 * the private {@link FileStorage}; every field carries provenance and a
 * deterministic confidence score. Money is parsed via {@link MoneyGrammar}
 * (exact BigDecimal, scale ≤ 2) and humans approve before posting.
 */
@Service
public class InvoiceDocumentService {

    static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    static final int RETENTION_DAYS = 180;
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    private final InvoiceDocumentRepository documents;
    private final InvoiceDocumentExtractionRepository extractions;
    private final InvoiceDocumentFieldRepository fields;
    private final InvoiceRepository invoices;
    private final InvoiceLineRepository lines;
    private final FileStorage storage;
    private final PdfTextExtractor extractor;
    private final OcrEngine ocrEngine;
    private final AiProvider aiProvider;
    private final AiUsageLogRepository usageLogs;
    private final AuditPublisher audit;
    private final MalwareScanner malwareScanner;
    private final TenantQuotaRepository quotaRepository;
    private final InvoiceDocumentJobRepository jobRepository;

    private final int maxMonthlyUploads;
    private final int maxMonthlyExtractions;
    private final List<String> allowedAiProviders;

    public InvoiceDocumentService(
            InvoiceDocumentRepository documents,
            InvoiceDocumentExtractionRepository extractions,
            InvoiceDocumentFieldRepository fields,
            InvoiceRepository invoices,
            InvoiceLineRepository lines,
            FileStorage storage,
            PdfTextExtractor extractor,
            OcrEngine ocrEngine,
            AiProvider aiProvider,
            AiUsageLogRepository usageLogs,
            AuditPublisher audit,
            MalwareScanner malwareScanner,
            TenantQuotaRepository quotaRepository,
            InvoiceDocumentJobRepository jobRepository,
            @Value("${fixna.invoice.quotas.monthly-uploads:100}") int maxMonthlyUploads,
            @Value("${fixna.invoice.quotas.monthly-extractions:200}") int maxMonthlyExtractions,
            @Value("${fixna.ai.document-processing.allowed-providers:}") List<String> allowedAiProviders) {
        this.documents = documents;
        this.extractions = extractions;
        this.fields = fields;
        this.invoices = invoices;
        this.lines = lines;
        this.storage = storage;
        this.extractor = extractor;
        this.ocrEngine = ocrEngine;
        this.aiProvider = aiProvider;
        this.usageLogs = usageLogs;
        this.audit = audit;
        this.malwareScanner = malwareScanner;
        this.quotaRepository = quotaRepository;
        this.jobRepository = jobRepository;
        this.maxMonthlyUploads = maxMonthlyUploads;
        this.maxMonthlyExtractions = maxMonthlyExtractions;
        this.allowedAiProviders = allowedAiProviders;
    }

    @Transactional
    public InvoiceDocumentResponse upload(UUID invoiceId, MultipartFile file) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        Invoice invoice = requireInvoice(invoiceId);

        if (file == null || file.isEmpty()) {
            throw new FixnaException("INVALID_FILE", HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new FixnaException("FILE_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE,
                    "Uploads are limited to 10 MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.equalsIgnoreCase("application/pdf")) {
            throw new FixnaException("UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF uploads are supported");
        }

        byte[] bytes;
        try (InputStream in = file.getInputStream()) {
            bytes = in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new FixnaException("INVALID_FILE", HttpStatus.BAD_REQUEST, "Unable to read upload");
        }
        if (!hasPdfMagic(bytes)) {
            throw new FixnaException("INVALID_FILE", HttpStatus.BAD_REQUEST,
                    "File content is not a PDF");
        }
        String sha256 = sha256Hex(bytes);
        if (documents.existsByInvoiceIdAndTenantIdAndSha256(invoice.getId(), tenantId, sha256)) {
            throw new FixnaException("DUPLICATE_DOCUMENT", HttpStatus.CONFLICT,
                    "This document was already uploaded");
        }

        // Check upload quota
        checkAndIncrementUploadQuota(tenantId);

        // Malware scan
        ScanResult scanResult = malwareScanner.scan(bytes);
        if (scanResult == ScanResult.INFECTED) {
            audit.publish(new AuditEvent(
                    "invoice.document.malware_detected",
                    tenantId,
                    userId,
                    "invoice_document",
                    null,
                    Map.of("fileName", file.getOriginalFilename(), "sizeBytes", String.valueOf(bytes.length)),
                    null));
            throw new FixnaException("MALWARE_DETECTED", HttpStatus.BAD_REQUEST,
                    "File contains malware and was rejected");
        } else if (scanResult == ScanResult.ERROR) {
            audit.publish(new AuditEvent(
                    "invoice.document.malware_scan_error",
                    tenantId,
                    userId,
                    "invoice_document",
                    null,
                    Map.of("fileName", file.getOriginalFilename(), "sizeBytes", String.valueOf(bytes.length)),
                    null));
            throw new FixnaException("MALWARE_SCAN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Malware scanner failed");
        }

        FileStorage.StoredFile stored = storage.store(tenantId, file.getOriginalFilename(),
                new java.io.ByteArrayInputStream(bytes), bytes.length);

        InvoiceDocument document = new InvoiceDocument();
        document.setId(UUID.randomUUID());
        document.setTenantId(tenantId);
        document.setInvoiceId(invoice.getId());
        document.setFileName(sanitizeName(file.getOriginalFilename()));
        document.setStorageKey(stored.storageKey());
        document.setContentType("application/pdf");
        document.setSizeBytes(bytes.length);
        document.setSha256(sha256);
        document.setStatus(InvoiceDocumentStatus.RECEIVED);
        document.setUploadedByUserId(userId);
        documents.save(document);

        audit.publish(new AuditEvent(
                "invoice.document.uploaded",
                tenantId,
                userId,
                "invoice_document",
                document.getId().toString(),
                Map.of("invoiceId", invoice.getId().toString(), "sizeBytes", String.valueOf(bytes.length)),
                null));
        return InvoiceDocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDocumentResponse> list(UUID invoiceId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        requireInvoice(invoiceId);
        UUID tenantId = TenantContext.requireTenantId();
        return documents.findByInvoiceIdAndTenantIdOrderByCreatedAtDesc(invoiceId, tenantId).stream()
                .map(InvoiceDocumentResponse::from)
                .toList();
    }

    /** Streams stored bytes; tenant is checked before any read. */
    @Transactional(readOnly = true)
    public byte[] download(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocument document = requireDocument(invoiceId, documentId);
        try (InputStream in = storage.open(tenantId, document.getStorageKey())) {
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new FixnaException("FILE_OPEN_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to open file");
        }
    }

    @Transactional(readOnly = true)
    public InvoiceDocument requireDocument(UUID invoiceId, UUID documentId) {
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocument document = documents.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Document not found"));
        if (!document.getInvoiceId().equals(invoiceId)) {
            throw new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Document not found");
        }
        return document;
    }

    @Transactional
    public void delete(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocument document = requireDocument(invoiceId, documentId);
        if (document.getStatus() == InvoiceDocumentStatus.DELETED) {
            return;
        }
        // Hard delete: drop bytes, fields, extraction rows; tombstone the document.
        List<InvoiceDocumentExtraction> docExtractions =
                extractions.findByDocumentIdAndTenantIdOrderByCreatedAtDesc(documentId, tenantId);
        for (InvoiceDocumentExtraction extraction : docExtractions) {
            fields.deleteByExtractionId(extraction.getId());
            extractions.delete(extraction);
        }
        deleteStorageQuietly(tenantId, document.getStorageKey());
        document.setStorageKey(null);
        document.setStatus(InvoiceDocumentStatus.DELETED);
        document.setPurgeAfter(null);
        documents.save(document);
        audit.publish(new AuditEvent(
                "invoice.document.deleted",
                tenantId,
                userId,
                "invoice_document",
                documentId.toString(),
                Map.of(),
                null));
    }

    @Transactional
    public InvoiceDocumentJobResponse extract(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocument document = requireDocument(invoiceId, documentId);
        if (document.getStatus() == InvoiceDocumentStatus.DELETED) {
            throw new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Document not found");
        }

        // Check extraction quota
        checkAndIncrementExtractionQuota(tenantId);

        // Create extraction job
        InvoiceDocumentJob job = new InvoiceDocumentJob();
        job.setTenantId(tenantId);
        job.setDocumentId(document.getId());
        job.setStatus(InvoiceDocumentJobStatus.QUEUED);
        jobRepository.save(job);

        // Update document status to indicate extraction is queued
        document.setStatus(InvoiceDocumentStatus.QUEUED_FOR_EXTRACTION);
        documents.save(document);

        audit.publish(new AuditEvent(
                "invoice.document.extraction_queued",
                tenantId,
                userId,
                "invoice_document_job",
                job.getId().toString(),
                Map.of("documentId", document.getId().toString()),
                null));

        return new InvoiceDocumentJobResponse(job.getId(), job.getStatus(), job.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public InvoiceExtractionResponse getExtraction(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocumentExtraction extraction = requireExtraction(invoiceId, documentId);
        List<InvoiceFieldResponse> fieldResponses =
                fields.findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId).stream()
                        .map(InvoiceFieldResponse::from)
                        .toList();
        return InvoiceExtractionResponse.from(extraction, fieldResponses);
    }

    @Transactional(readOnly = true)
    public in.fixna.platform.common.web.PageResponse<InvoiceExtractionSummary> searchExtractions(
            String query,
            InvoiceExtractionStatus status,
            java.time.OffsetDateTime dateFrom,
            java.time.OffsetDateTime dateTo,
            String currency,
            int page,
            int size) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var result = extractions.searchByTenant(tenantId, status, dateFrom, dateTo, currency, query, pageable);

        List<InvoiceExtractionSummary> summaries = result.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new in.fixna.platform.common.web.PageResponse<>(
                summaries,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private InvoiceExtractionSummary toSummary(InvoiceDocumentExtraction extraction) {
        UUID tenantId = TenantContext.requireTenantId();
        var fieldList = fields.findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId);
        int fieldCount = fieldList.size();
        BigDecimal totalAmount = fieldList.stream()
                .filter(f -> "total".equals(f.getFieldKey()) && f.getMoneyAmount() != null)
                .map(InvoiceDocumentField::getMoneyAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        // Get invoiceId from the document
        UUID invoiceId = documents.findByIdAndTenantId(extraction.getDocumentId(), tenantId)
                .map(InvoiceDocument::getInvoiceId)
                .orElse(null);
        return new InvoiceExtractionSummary(
                extraction.getId(),
                extraction.getDocumentId(),
                invoiceId,
                extraction.getStatus(),
                extraction.getCreatedAt(),
                fieldCount,
                totalAmount);
    }

    /** Confirms/edits a field; sets source=MANUAL and records the actor. */
    @Transactional
    public InvoiceFieldResponse confirmField(UUID invoiceId, UUID documentId, UUID fieldId,
            InvoiceFieldConfirmRequest request) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocumentExtraction extraction = requireExtraction(invoiceId, documentId);
        if (extraction.getStatus().isTerminal()) {
            throw new FixnaException("INVALID_STATE", HttpStatus.CONFLICT,
                    "Extraction is already terminal");
        }
        InvoiceDocumentField field = fields.findByIdAndTenantId(fieldId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Field not found"));
        if (!field.getExtractionId().equals(extraction.getId())) {
            throw new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Field not found");
        }
        String newValue = request.fieldValue() == null ? field.getFieldValue() : request.fieldValue().trim();
        field.setFieldValue(newValue);
        if (field.getMoneyAmount() != null) {
            MoneyGrammar.parse(newValue).ifPresentOrElse(
                    field::setMoneyAmount,
                    () -> {
                        throw new FixnaException("FIELD_NOT_DECIMAL", HttpStatus.UNPROCESSABLE_ENTITY,
                                "Monetary value must be an exact decimal with at most 2 fraction digits");
                    });
        }
        field.setSource(FieldSource.MANUAL);
        field.setConfirmedByUserId(userId);
        fields.save(field);
        return InvoiceFieldResponse.from(field);
    }

    @Transactional
    public InvoiceExtractionResponse approve(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocumentExtraction extraction = requireExtraction(invoiceId, documentId);
        if (extraction.getStatus() != InvoiceExtractionStatus.NEEDS_REVIEW) {
            throw new FixnaException("INVALID_STATE", HttpStatus.CONFLICT, "Only pending extractions can be approved");
        }
        List<InvoiceDocumentField> fieldList =
                fields.findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId);
        List<String> blocking = fieldList.stream()
                .filter(f -> f.getSource() != FieldSource.AI_ADVISORY)
                .filter(f -> ConfidenceGate.isVisible(f.getConfidence(), true))
                .filter(f -> ConfidenceGate.blocksApprovalUntilConfirmed(
                        f.getConfidence(), true, f.getConfirmedByUserId() != null))
                .map(InvoiceDocumentField::getFieldKey)
                .toList();
        if (!blocking.isEmpty()) {
            throw new FixnaException("GATES_UNMET", HttpStatus.CONFLICT,
                    "Confirm low-confidence fields before approval: " + String.join(", ", blocking));
        }
        extraction.setStatus(InvoiceExtractionStatus.APPROVED);
        extraction.setApprovedByUserId(userId);
        extraction.setApprovedAt(OffsetDateTime.now());
        extractions.save(extraction);
        requireDocument(invoiceId, documentId).setStatus(InvoiceDocumentStatus.APPROVED);
        documents.save(requireDocument(invoiceId, documentId));

        audit.publish(new AuditEvent(
                "invoice.review.approved",
                tenantId,
                userId,
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of("documentId", documentId.toString()),
                null));
        return getExtraction(invoiceId, documentId);
    }

    @Transactional
    public InvoiceExtractionResponse reject(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocumentExtraction extraction = requireExtraction(invoiceId, documentId);
        if (extraction.getStatus().isTerminal()) {
            throw new FixnaException("INVALID_STATE", HttpStatus.CONFLICT, "Extraction is already terminal");
        }
        extraction.setStatus(InvoiceExtractionStatus.REJECTED);
        extractions.save(extraction);
        InvoiceDocument document = requireDocument(invoiceId, documentId);
        document.setStatus(InvoiceDocumentStatus.REJECTED);
        documents.save(document);
        audit.publish(new AuditEvent(
                "invoice.review.rejected",
                tenantId,
                userId,
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of(),
                null));
        return getExtraction(invoiceId, documentId);
    }

    /**
     * Posts an approved extraction to the invoice as a single line item.
     * Only DRAFT (editable) invoices can be posted to; AI never posts.
     */
    @Transactional
    public InvoiceExtractionResponse post(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocumentExtraction extraction = requireExtraction(invoiceId, documentId);
        if (extraction.getStatus() != InvoiceExtractionStatus.APPROVED) {
            throw new FixnaException("INVALID_STATE", HttpStatus.CONFLICT,
                    "Only approved extractions can be posted");
        }
        Invoice invoice = requireInvoice(invoiceId);
        if (!invoice.getStatus().isEditable()) {
            throw new FixnaException("INVOICE_NOT_EDITABLE", HttpStatus.BAD_REQUEST,
                    "Only draft invoices can be posted to");
        }
        InvoiceDocumentField total = fields
                .findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId).stream()
                .filter(f -> "total".equals(f.getFieldKey()) && f.getMoneyAmount() != null)
                .findFirst()
                .orElseThrow(() -> new FixnaException("MISSING_TOTAL", HttpStatus.UNPROCESSABLE_ENTITY,
                        "Approved extraction has no total to post"));

        InvoiceLine line = new InvoiceLine();
        line.setId(UUID.randomUUID());
        line.setTenantId(tenantId);
        line.setInvoiceId(invoice.getId());
        line.setDescription("Imported from document " + requireDocument(invoiceId, documentId).getFileName());
        line.setQuantity(java.math.BigDecimal.ONE);
        line.setUnitPrice(total.getMoneyAmount());
        line.setLineTotal(total.getMoneyAmount());
        lines.save(line);

        extraction.setStatus(InvoiceExtractionStatus.POSTED);
        extraction.setPostedInvoiceId(invoice.getId());
        extractions.save(extraction);
        InvoiceDocument document = requireDocument(invoiceId, documentId);
        document.setStatus(InvoiceDocumentStatus.POSTED);
        documents.save(document);

        audit.publish(new AuditEvent(
                "invoice.review.posted",
                tenantId,
                userId,
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of("invoiceId", invoice.getId().toString(),
                        "amount", total.getMoneyAmount().toPlainString()),
                null));
        return getExtraction(invoiceId, documentId);
    }

    /** Exports an extraction as CSV (one row per field) — audited. */
    @Transactional(readOnly = true)
    public String exportCsv(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocumentExtraction extraction = requireExtraction(invoiceId, documentId);
        List<InvoiceDocumentField> fieldList =
                fields.findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId);
        StringBuilder csv = new StringBuilder();
        csv.append("field_key,field_value,money_amount,currency,confidence,source,page\n");
        for (InvoiceDocumentField field : fieldList) {
            csv.append(csvCell(field.getFieldKey())).append(',')
                    .append(csvCell(field.getFieldValue())).append(',')
                    .append(field.getMoneyAmount() == null ? "" : field.getMoneyAmount().toPlainString())
                    .append(',')
                    .append(csvCell(field.getCurrency())).append(',')
                    .append(field.getConfidence() == null ? "" : field.getConfidence().toPlainString())
                    .append(',')
                    .append(field.getSource().name()).append(',')
                    .append(field.getPage() == null ? "" : field.getPage())
                    .append('\n');
        }
        audit.publish(new AuditEvent(
                "invoice.export.generated",
                tenantId,
                TenantContext.requireUserId(),
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of("format", "csv"),
                null));
        return csv.toString();
    }

    /** Exports an extraction as a JSON snapshot (full fidelity) — audited. */
    @Transactional(readOnly = true)
    public String exportJson(UUID invoiceId, UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocumentExtraction extraction = requireExtraction(invoiceId, documentId);
        List<InvoiceDocumentField> fieldList =
                fields.findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId);
        StringBuilder json = new StringBuilder();
        json.append("{\"extractionId\":\"").append(extraction.getId()).append("\",");
        json.append("\"documentId\":\"").append(extraction.getDocumentId()).append("\",");
        json.append("\"status\":\"").append(extraction.getStatus().name()).append("\",");
        json.append("\"methodNativePages\":").append(extraction.getMethodNativePages()).append(',');
        json.append("\"methodOcrPages\":").append(extraction.getMethodOcrPages()).append(',');
        json.append("\"extractorVersion\":\"").append(jsonEscape(extraction.getExtractorVersion())).append("\",");
        json.append("\"fields\":[");
        for (int i = 0; i < fieldList.size(); i++) {
            InvoiceDocumentField field = fieldList.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"fieldKey\":\"").append(jsonEscape(field.getFieldKey())).append("\",");
            json.append("\"fieldValue\":").append(jsonString(field.getFieldValue())).append(',');
            json.append("\"moneyAmount\":")
                    .append(field.getMoneyAmount() == null ? "null" : '"' + field.getMoneyAmount().toPlainString() + '"')
                    .append(',');
            json.append("\"currency\":").append(jsonString(field.getCurrency())).append(',');
            json.append("\"confidence\":")
                    .append(field.getConfidence() == null ? "null" : '"' + field.getConfidence().toPlainString() + '"')
                    .append(',');
            json.append("\"source\":\"").append(field.getSource().name()).append("\",");
            json.append("\"page\":").append(field.getPage() == null ? "null" : field.getPage()).append(',');
            json.append("\"rawSnippet\":").append(jsonString(field.getRawSnippet()));
            json.append('}');
        }
        json.append("]}");
        audit.publish(new AuditEvent(
                "invoice.export.generated",
                tenantId,
                TenantContext.requireUserId(),
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of("format", "json"),
                null));
        return json.toString();
    }

    private static String jsonString(String value) {
        return value == null ? "null" : '"' + jsonEscape(value) + '"';
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /** Optional advisory AI line ideas; never applied automatically. */
    @Transactional
    public in.fixna.platform.ai.dto.AiSuggestionResponse<InvoiceLineIdeasSuggestion> suggestLineIdeas(
            UUID invoiceId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        TenantContext.requirePermission(in.fixna.platform.rbac.Permission.AI_USE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        // Check if AI provider is allowed
        String providerName = "mock"; // Current implementation uses MockAiProvider
        if (!isAiProviderAllowed(tenantId, userId, providerName)) {
            // Fail-open: return empty suggestion instead of blocking
            return in.fixna.platform.ai.dto.AiSuggestionResponse.of(
                    new InvoiceLineIdeasSuggestion(List.of()));
        }

        Invoice invoice = requireInvoice(invoiceId);
        List<String> existing = lines.findByInvoiceIdAndTenantIdOrderBySortOrderAsc(invoice.getId(), tenantId)
                .stream().map(InvoiceLine::getDescription).toList();
        InvoiceLineIdeasSuggestion ideas = aiProvider.suggestInvoiceLineIdeas(invoice.getTitle(), existing);
        AiUsageLog log = new AiUsageLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setFeature(AiFeature.INVOICE_LINE_IDEAS);
        log.setRequestSummary("invoiceId=" + invoiceId);
        usageLogs.save(log);
        return in.fixna.platform.ai.dto.AiSuggestionResponse.of(ideas);
    }

    private InvoiceDocumentExtraction requireExtraction(UUID invoiceId, UUID documentId) {
        InvoiceDocument document = requireDocument(invoiceId, documentId);
        UUID tenantId = TenantContext.requireTenantId();
        return extractions.findFirstByDocumentIdAndTenantIdOrderByCreatedAtDesc(document.getId(), tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "No extraction for this document"));
    }

    private Invoice requireInvoice(UUID invoiceId) {
        UUID tenantId = TenantContext.requireTenantId();
        return invoices.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    /**
     * Checks if the tenant has exceeded their monthly upload quota and increments the counter if not.
     * @throws FixnaException with QUOTA_EXCEEDED if quota is exceeded
     */
    private void checkAndIncrementUploadQuota(UUID tenantId) {
        TenantQuota quota = quotaRepository.findById(tenantId)
                .map(existing -> {
                    if (existing.getUploadsThisMonth() >= getMaxMonthlyUploads()) {
                        throw new FixnaException("QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS,
                                "Monthly upload quota exceeded");
                    }
                    // Increment and save
                    existing.setUploadsThisMonth(existing.getUploadsThisMonth() + 1);
                    quotaRepository.save(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    // Create new quota record if none exists
                    TenantQuota newQuota = new TenantQuota(tenantId);
                    newQuota.setUploadsThisMonth(1); // Starting with this upload
                    quotaRepository.save(newQuota);
                    return newQuota;
                });
    }

    private int getMaxMonthlyUploads() {
        return maxMonthlyUploads;
    }

    /**
     * Checks if the tenant has exceeded their monthly extraction quota and increments the counter if not.
     * @throws FixnaException with QUOTA_EXCEEDED if quota is exceeded
     */
    private void checkAndIncrementExtractionQuota(UUID tenantId) {
        TenantQuota quota = quotaRepository.findById(tenantId)
                .map(existing -> {
                    if (existing.getExtractionsThisMonth() >= getMaxMonthlyExtractions()) {
                        throw new FixnaException("QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS,
                                "Monthly extraction quota exceeded");
                    }
                    // Increment and save
                    existing.setExtractionsThisMonth(existing.getExtractionsThisMonth() + 1);
                    quotaRepository.save(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    // Create new quota record if none exists
                    TenantQuota newQuota = new TenantQuota(tenantId);
                    newQuota.setExtractionsThisMonth(1); // Starting with this extraction
                    quotaRepository.save(newQuota);
                    return newQuota;
                });
    }

    private int getMaxMonthlyExtractions() {
        return maxMonthlyExtractions;
    }

    /**
     * Checks if the current AI provider is allowed by the allowlist.
     * Returns true if allowed or if the allowlist is empty (fail-open for review).
     * Logs the decision for audit purposes.
     */
    private boolean isAiProviderAllowed(UUID tenantId, UUID userId, String providerName) {
        if (allowedAiProviders == null || allowedAiProviders.isEmpty()) {
            // Allowlist empty = no external providers allowed, but fail-open for review
            audit.publish(new AuditEvent(
                    "ai.provider.check",
                    tenantId,
                    userId,
                    "ai_provider",
                    providerName != null ? providerName : "unknown",
                    Map.of("decision", "blocked", "reason", "allowlist_empty"),
                    null));
            return false;
        }
        boolean allowed = allowedAiProviders.contains(providerName);
        audit.publish(new AuditEvent(
                "ai.provider.check",
                tenantId,
                userId,
                "ai_provider",
                providerName != null ? providerName : "unknown",
                Map.of("decision", allowed ? "allowed" : "blocked"),
                null));
        return allowed;
    }

    private static boolean hasPdfMagic(byte[] bytes) {
        if (bytes.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "document.pdf";
        }
        String cleaned = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.length() > 200 ? cleaned.substring(cleaned.length() - 200) : cleaned;
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void deleteStorageQuietly(UUID tenantId, String storageKey) {
        if (storageKey == null) {
            return;
        }
        try {
            storage.delete(tenantId, storageKey);
        } catch (RuntimeException ignored) {
            // best-effort: DB tombstone still records deletion intent
        }
    }
}