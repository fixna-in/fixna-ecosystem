package in.fixna.invoiceassistant.invoice;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import in.fixna.invoiceassistant.audit.AuditEvent;
import in.fixna.invoiceassistant.audit.AuditPublisher;
import in.fixna.invoiceassistant.config.TenantContext;
import in.fixna.invoiceassistant.document.ConfidenceGate;
import in.fixna.invoiceassistant.document.FieldSource;
import in.fixna.invoiceassistant.document.MalwareScanner;
import in.fixna.invoiceassistant.document.MoneyGrammar;
import in.fixna.invoiceassistant.document.OcrEngine;
import in.fixna.invoiceassistant.document.PdfTextExtractor;
import in.fixna.invoiceassistant.document.ScanResult;
import in.fixna.invoiceassistant.file.FileStorage;
import in.fixna.invoiceassistant.invoice.dto.InvoiceDocumentJobResponse;
import in.fixna.invoiceassistant.invoice.dto.InvoiceDocumentResponse;
import in.fixna.invoiceassistant.invoice.dto.InvoiceExtractionResponse;
import in.fixna.invoiceassistant.invoice.dto.InvoiceExtractionSummary;
import in.fixna.invoiceassistant.invoice.dto.InvoiceFieldConfirmRequest;
import in.fixna.invoiceassistant.invoice.dto.InvoiceFieldResponse;
import in.fixna.invoiceassistant.security.Permission;
import in.fixna.invoiceassistant.web.PageResponse;
import in.fixna.platform.common.web.FixnaException;

/**
 * Private invoice document intake (ADR-011) with provenance-backed
 * extraction (ADR-012) for the standalone Invoice Assistant product.
 * Upload is PDF-only and size-capped; bytes never leave the private
 * {@link FileStorage}; every field carries provenance and a deterministic
 * confidence score. Money is parsed via {@link MoneyGrammar} (exact
 * BigDecimal, scale ≤ 2) and humans approve before posting. AI is
 * advisory-only and not wired in this MVP — extraction is fully offline.
 */
@Service
public class InvoiceDocumentService {

    static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    static final int RETENTION_DAYS = 180;
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};
    private static final DateTimeFormatter EURO_DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final InvoiceDocumentRepository documents;
    private final InvoiceDocumentExtractionRepository extractions;
    private final InvoiceDocumentFieldRepository fields;
    private final InvoiceRecordRepository records;
    private final InvoiceRecordLineRepository lines;
    private final FileStorage storage;
    private final PdfTextExtractor extractor;
    private final OcrEngine ocrEngine;
    private final AuditPublisher audit;
    private final MalwareScanner malwareScanner;
    private final TenantQuotaRepository quotaRepository;
    private final InvoiceDocumentJobRepository jobRepository;

    private final int maxMonthlyUploads;
    private final int maxMonthlyExtractions;

    public InvoiceDocumentService(
            InvoiceDocumentRepository documents,
            InvoiceDocumentExtractionRepository extractions,
            InvoiceDocumentFieldRepository fields,
            InvoiceRecordRepository records,
            InvoiceRecordLineRepository lines,
            FileStorage storage,
            PdfTextExtractor extractor,
            OcrEngine ocrEngine,
            AuditPublisher audit,
            MalwareScanner malwareScanner,
            TenantQuotaRepository quotaRepository,
            InvoiceDocumentJobRepository jobRepository,
            @Value("${fixna.invoice.quotas.monthly-uploads:100}") int maxMonthlyUploads,
            @Value("${fixna.invoice.quotas.monthly-extractions:200}") int maxMonthlyExtractions) {
        this.documents = documents;
        this.extractions = extractions;
        this.fields = fields;
        this.records = records;
        this.lines = lines;
        this.storage = storage;
        this.extractor = extractor;
        this.ocrEngine = ocrEngine;
        this.audit = audit;
        this.malwareScanner = malwareScanner;
        this.quotaRepository = quotaRepository;
        this.jobRepository = jobRepository;
        this.maxMonthlyUploads = maxMonthlyUploads;
        this.maxMonthlyExtractions = maxMonthlyExtractions;
    }

    /**
     * Validates and stores a PDF upload. The document may stand alone or
     * link to an owned invoice record; extraction is queued separately.
     */
    @Transactional
    public InvoiceDocumentResponse upload(MultipartFile file, UUID invoiceRecordId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        if (invoiceRecordId != null) {
            requireRecord(invoiceRecordId);
        }

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
        boolean duplicate = invoiceRecordId != null
                ? documents.existsByTenantIdAndInvoiceRecordIdAndSha256(tenantId, invoiceRecordId, sha256)
                : documents.existsByTenantIdAndInvoiceRecordIdIsNullAndSha256(tenantId, sha256);
        if (duplicate) {
            throw new FixnaException("DUPLICATE_DOCUMENT", HttpStatus.CONFLICT,
                    "This document was already uploaded");
        }

        checkAndIncrementUploadQuota(tenantId);

        ScanResult scanResult = malwareScanner.scan(bytes);
        if (scanResult == ScanResult.INFECTED) {
            audit.publish(new AuditEvent(
                    "invoice.document.malware_detected",
                    tenantId,
                    userId,
                    "invoice_document",
                    null,
                    Map.of("fileName", String.valueOf(file.getOriginalFilename()),
                            "sizeBytes", String.valueOf(bytes.length))));
            throw new FixnaException("MALWARE_DETECTED", HttpStatus.BAD_REQUEST,
                    "File contains malware and was rejected");
        } else if (scanResult == ScanResult.ERROR) {
            audit.publish(new AuditEvent(
                    "invoice.document.malware_scan_error",
                    tenantId,
                    userId,
                    "invoice_document",
                    null,
                    Map.of("fileName", String.valueOf(file.getOriginalFilename()),
                            "sizeBytes", String.valueOf(bytes.length))));
            throw new FixnaException("MALWARE_SCAN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Malware scanner failed");
        }

        FileStorage.StoredFile stored = storage.store(tenantId, file.getOriginalFilename(),
                new ByteArrayInputStream(bytes), bytes.length);

        InvoiceDocument document = new InvoiceDocument();
        document.setId(UUID.randomUUID());
        document.setTenantId(tenantId);
        document.setInvoiceRecordId(invoiceRecordId);
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
                Map.of("sizeBytes", String.valueOf(bytes.length),
                        "invoiceRecordId", invoiceRecordId == null ? "" : invoiceRecordId.toString())));
        return InvoiceDocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceDocumentResponse> list(UUID invoiceRecordId, int page, int size) {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        List<InvoiceDocumentResponse> content =
                (invoiceRecordId != null
                                ? documents.findByTenantIdAndInvoiceRecordIdOrderByCreatedAtDesc(
                                        tenantId, invoiceRecordId)
                                : documents.findByTenantIdOrderByCreatedAtDesc(tenantId))
                        .stream()
                        .map(InvoiceDocumentResponse::from)
                        .toList();
        return new PageResponse<>(content, page, size, content.size(), 1);
    }

    @Transactional(readOnly = true)
    public InvoiceDocumentResponse get(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        return InvoiceDocumentResponse.from(requireDocument(documentId));
    }

    /** Streams stored bytes; tenant is checked before any read. */
    @Transactional(readOnly = true)
    public byte[] download(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocument document = requireDocument(documentId);
        try (InputStream in = storage.open(tenantId, document.getStorageKey())) {
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new FixnaException("FILE_OPEN_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to open file");
        }
    }

    @Transactional(readOnly = true)
    public InvoiceDocument requireDocument(UUID documentId) {
        UUID tenantId = TenantContext.requireTenantId();
        return documents.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Document not found"));
    }

    /** Hard delete: drop bytes, fields, extraction rows; tombstone the document. */
    @Transactional
    public void delete(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocument document = requireDocument(documentId);
        if (document.getStatus() == InvoiceDocumentStatus.DELETED) {
            return;
        }
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
                Map.of()));
    }

    /** Queues an async extraction job; the worker processes it off-request. */
    @Transactional
    public InvoiceDocumentJobResponse extract(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocument document = requireDocument(documentId);
        if (document.getStatus() == InvoiceDocumentStatus.DELETED) {
            throw new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Document not found");
        }
        if (document.getStatus().isTerminal()) {
            throw new FixnaException("INVALID_STATE", HttpStatus.CONFLICT,
                    "Document is already " + document.getStatus());
        }

        checkAndIncrementExtractionQuota(tenantId);

        InvoiceDocumentJob job = new InvoiceDocumentJob();
        job.setTenantId(tenantId);
        job.setDocumentId(document.getId());
        job.setStatus(InvoiceDocumentJobStatus.QUEUED);
        jobRepository.save(job);

        audit.publish(new AuditEvent(
                "invoice.document.extraction_queued",
                tenantId,
                userId,
                "invoice_document_job",
                job.getId().toString(),
                Map.of("documentId", document.getId().toString())));

        return new InvoiceDocumentJobResponse(
                job.getId(), job.getDocumentId(), job.getStatus(), null, null, job.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public InvoiceExtractionResponse getExtraction(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocumentExtraction extraction = requireExtraction(documentId);
        List<InvoiceFieldResponse> fieldResponses =
                fields.findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId).stream()
                        .map(InvoiceFieldResponse::from)
                        .toList();
        return InvoiceExtractionResponse.from(extraction, fieldResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceExtractionSummary> searchExtractions(
            String query,
            InvoiceExtractionStatus status,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            String currency,
            int page,
            int size) {
        TenantContext.requirePermission(Permission.INVOICE_VIEW);
        UUID tenantId = TenantContext.requireTenantId();

        Page<InvoiceDocumentExtraction> result = extractions.searchByTenant(
                tenantId, status, dateFrom, dateTo, currency, query, PageRequest.of(page, size));

        List<InvoiceExtractionSummary> summaries = result.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(
                summaries,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private InvoiceExtractionSummary toSummary(InvoiceDocumentExtraction extraction) {
        UUID tenantId = TenantContext.requireTenantId();
        List<InvoiceDocumentField> fieldList =
                fields.findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId);
        int fieldCount = fieldList.size();
        BigDecimal totalAmount = fieldList.stream()
                .filter(f -> "total".equals(f.getFieldKey()) && f.getMoneyAmount() != null)
                .map(InvoiceDocumentField::getMoneyAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        UUID invoiceRecordId = documents.findByIdAndTenantId(extraction.getDocumentId(), tenantId)
                .map(InvoiceDocument::getInvoiceRecordId)
                .orElse(null);
        return new InvoiceExtractionSummary(
                extraction.getId(),
                extraction.getDocumentId(),
                invoiceRecordId,
                extraction.getStatus(),
                extraction.getCreatedAt(),
                fieldCount,
                totalAmount);
    }

    /** Confirms/edits a field; sets source=MANUAL and records the actor. */
    @Transactional
    public InvoiceFieldResponse confirmField(UUID documentId, UUID fieldId,
            InvoiceFieldConfirmRequest request) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocumentExtraction extraction = requireExtraction(documentId);
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
    public InvoiceExtractionResponse approve(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocumentExtraction extraction = requireExtraction(documentId);
        if (extraction.getStatus() != InvoiceExtractionStatus.NEEDS_REVIEW) {
            throw new FixnaException("INVALID_STATE", HttpStatus.CONFLICT,
                    "Only pending extractions can be approved");
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
        InvoiceDocument document = requireDocument(documentId);
        document.setStatus(InvoiceDocumentStatus.APPROVED);
        documents.save(document);

        audit.publish(new AuditEvent(
                "invoice.review.approved",
                tenantId,
                userId,
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of("documentId", documentId.toString())));
        return getExtraction(documentId);
    }

    @Transactional
    public InvoiceExtractionResponse reject(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocumentExtraction extraction = requireExtraction(documentId);
        if (extraction.getStatus().isTerminal()) {
            throw new FixnaException("INVALID_STATE", HttpStatus.CONFLICT, "Extraction is already terminal");
        }
        extraction.setStatus(InvoiceExtractionStatus.REJECTED);
        extractions.save(extraction);
        InvoiceDocument document = requireDocument(documentId);
        document.setStatus(InvoiceDocumentStatus.REJECTED);
        documents.save(document);
        audit.publish(new AuditEvent(
                "invoice.review.rejected",
                tenantId,
                userId,
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of()));
        return getExtraction(documentId);
    }

    /**
     * Posts an approved extraction to an owned invoice record. A linked
     * editable record is updated in place; otherwise a new record is created.
     * Only human-approved extractions post; AI never posts.
     */
    @Transactional
    public InvoiceExtractionResponse post(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        InvoiceDocumentExtraction extraction = requireExtraction(documentId);
        if (extraction.getStatus() != InvoiceExtractionStatus.APPROVED) {
            throw new FixnaException("INVALID_STATE", HttpStatus.CONFLICT,
                    "Only approved extractions can be posted");
        }
        InvoiceDocument document = requireDocument(documentId);
        List<InvoiceDocumentField> fieldList =
                fields.findByExtractionIdAndTenantIdOrderByFieldKeyAsc(extraction.getId(), tenantId);
        BigDecimal total = fieldValue(fieldList, "total")
                .flatMap(f -> Optional.ofNullable(f.getMoneyAmount()))
                .orElseThrow(() -> new FixnaException("MISSING_TOTAL", HttpStatus.UNPROCESSABLE_ENTITY,
                        "Approved extraction has no total to post"));

        InvoiceRecord record;
        if (document.getInvoiceRecordId() != null) {
            record = requireRecord(document.getInvoiceRecordId());
            if (!record.getStatus().isEditable()) {
                throw new FixnaException("INVOICE_NOT_EDITABLE", HttpStatus.BAD_REQUEST,
                        "Only draft or needs-review records can be posted to");
            }
        } else {
            record = new InvoiceRecord();
            record.setTenantId(tenantId);
            record.setCreatedByUserId(userId);
            record.setStatus(InvoiceRecordStatus.DRAFT);
        }
        fieldValue(fieldList, "vendor").ifPresent(f -> record.setVendorName(f.getFieldValue()));
        fieldValue(fieldList, "invoice_number").ifPresent(f -> record.setInvoiceNumber(f.getFieldValue()));
        fieldValue(fieldList, "invoice_date").flatMap(f -> parseDate(f.getFieldValue()))
                .ifPresent(record::setInvoiceDate);
        fieldValue(fieldList, "due_date").flatMap(f -> parseDate(f.getFieldValue()))
                .ifPresent(record::setDueDate);
        fieldValue(fieldList, "currency").flatMap(f -> Optional.ofNullable(f.getCurrency()))
                .ifPresent(record::setCurrency);
        fieldValue(fieldList, "subtotal").flatMap(f -> Optional.ofNullable(f.getMoneyAmount()))
                .ifPresent(record::setSubtotal);
        fieldValue(fieldList, "tax").flatMap(f -> Optional.ofNullable(f.getMoneyAmount()))
                .ifPresent(record::setTaxTotal);
        record.setGrandTotal(total);
        record.setStatus(InvoiceRecordStatus.POSTED);
        records.save(record);

        InvoiceRecordLine line = new InvoiceRecordLine();
        line.setId(UUID.randomUUID());
        line.setTenantId(tenantId);
        line.setInvoiceRecordId(record.getId());
        line.setDescription("Imported from document " + document.getFileName());
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(total);
        line.setLineTotal(total);
        line.setSortOrder(0);
        lines.save(line);

        document.setInvoiceRecordId(record.getId());
        document.setStatus(InvoiceDocumentStatus.POSTED);
        documents.save(document);
        extraction.setStatus(InvoiceExtractionStatus.POSTED);
        extraction.setPostedInvoiceRecordId(record.getId());
        extractions.save(extraction);

        audit.publish(new AuditEvent(
                "invoice.review.posted",
                tenantId,
                userId,
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of("invoiceRecordId", record.getId().toString(),
                        "amount", total.toPlainString())));
        return getExtraction(documentId);
    }

    /** Exports an extraction as CSV (one row per field) — audited. */
    @Transactional(readOnly = true)
    public String exportCsv(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocumentExtraction extraction = requireExtraction(documentId);
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
                Map.of("format", "csv")));
        return csv.toString();
    }

    /** Exports an extraction as a JSON snapshot (full fidelity) — audited. */
    @Transactional(readOnly = true)
    public String exportJson(UUID documentId) {
        TenantContext.requirePermission(Permission.INVOICE_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        InvoiceDocumentExtraction extraction = requireExtraction(documentId);
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
                    .append(field.getMoneyAmount() == null
                            ? "null" : '"' + field.getMoneyAmount().toPlainString() + '"')
                    .append(',');
            json.append("\"currency\":").append(jsonString(field.getCurrency())).append(',');
            json.append("\"confidence\":")
                    .append(field.getConfidence() == null
                            ? "null" : '"' + field.getConfidence().toPlainString() + '"')
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
                Map.of("format", "json")));
        return json.toString();
    }

    private InvoiceDocumentExtraction requireExtraction(UUID documentId) {
        InvoiceDocument document = requireDocument(documentId);
        UUID tenantId = TenantContext.requireTenantId();
        return extractions.findFirstByDocumentIdAndTenantIdOrderByCreatedAtDesc(document.getId(), tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "No extraction for this document"));
    }

    private InvoiceRecord requireRecord(UUID recordId) {
        UUID tenantId = TenantContext.requireTenantId();
        return records.findByIdAndTenantId(recordId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Invoice record not found"));
    }

    private static Optional<InvoiceDocumentField> fieldValue(
            List<InvoiceDocumentField> fieldList, String key) {
        return fieldList.stream()
                .filter(f -> key.equals(f.getFieldKey()))
                .findFirst();
    }

    private static Optional<LocalDate> parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();
        try {
            return Optional.of(LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (DateTimeParseException ignored) {
            // fall through to dd/MM/yyyy (and dd.MM.yyyy) form
        }
        String slashed = value.replace('.', '/').replace('-', '/');
        try {
            return Optional.of(LocalDate.parse(slashed, EURO_DATE));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /** Checks and increments the monthly upload quota; {@code QUOTA_EXCEEDED} when over. */
    private void checkAndIncrementUploadQuota(UUID tenantId) {
        quotaRepository.findById(tenantId)
                .map(existing -> {
                    if (existing.getUploadsThisMonth() >= maxMonthlyUploads) {
                        throw new FixnaException("QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS,
                                "Monthly upload quota exceeded");
                    }
                    existing.setUploadsThisMonth(existing.getUploadsThisMonth() + 1);
                    quotaRepository.save(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    TenantQuota newQuota = new TenantQuota(tenantId);
                    newQuota.setUploadsThisMonth(1);
                    quotaRepository.save(newQuota);
                    return newQuota;
                });
    }

    /** Checks and increments the monthly extraction quota; {@code QUOTA_EXCEEDED} when over. */
    private void checkAndIncrementExtractionQuota(UUID tenantId) {
        quotaRepository.findById(tenantId)
                .map(existing -> {
                    if (existing.getExtractionsThisMonth() >= maxMonthlyExtractions) {
                        throw new FixnaException("QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS,
                                "Monthly extraction quota exceeded");
                    }
                    existing.setExtractionsThisMonth(existing.getExtractionsThisMonth() + 1);
                    quotaRepository.save(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    TenantQuota newQuota = new TenantQuota(tenantId);
                    newQuota.setExtractionsThisMonth(1);
                    quotaRepository.save(newQuota);
                    return newQuota;
                });
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

    private static String jsonString(String value) {
        return value == null ? "null" : '"' + jsonEscape(value) + '"';
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
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

