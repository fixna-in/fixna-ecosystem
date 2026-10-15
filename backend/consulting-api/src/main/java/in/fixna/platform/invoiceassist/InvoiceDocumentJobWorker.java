package in.fixna.platform.invoiceassist;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import in.fixna.platform.invoiceassist.FieldExtractor.Draft;
import in.fixna.platform.invoiceassist.PdfTextExtractor.PageText;
import in.fixna.platform.tenancy.TenantContext;

import io.jsonwebtoken.Jwts;

/**
 * Worker that processes queued invoice document extraction jobs.
 * Runs every 10 seconds (configurable via fixna.invoice.job-worker.poll-ms).
 */
@Component
public class InvoiceDocumentJobWorker {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.invoice.job.worker");

    private final InvoiceDocumentJobRepository jobRepository;
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

    public InvoiceDocumentJobWorker(
            InvoiceDocumentJobRepository jobRepository,
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
            AuditPublisher audit) {
        this.jobRepository = jobRepository;
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
    }

    /**
     * Polls for QUEUED jobs and processes them.
     * Runs every 10 seconds by default (configurable via fixna.invoice.job-worker.poll-ms).
     */
    @Scheduled(fixedRateString = "${fixna.invoice.job-worker.poll-ms:10000}")
    @Transactional
    public void processQueuedJobs() {
        List<InvoiceDocumentJob> queuedJobs = jobRepository.findQueuedJobs(InvoiceDocumentJobStatus.QUEUED);
        if (queuedJobs.isEmpty()) {
            return;
        }
        LOG.debug("Found {} queued extraction jobs", queuedJobs.size());
        for (InvoiceDocumentJob job : queuedJobs) {
            try {
                processJob(job);
            } catch (Exception e) {
                LOG.error("Failed to process extraction job {}", job.getId(), e);
                failJob(job, "PROCESSING_ERROR", e.getMessage());
            }
        }
    }

    private void processJob(InvoiceDocumentJob job) {
        UUID tenantId = job.getTenantId();
        UUID documentId = job.getDocumentId();

        // Claim the job (set status to RUNNING)
        int claimed = jobRepository.claimJob(job.getId(), OffsetDateTime.now(),
                InvoiceDocumentJobStatus.QUEUED, InvoiceDocumentJobStatus.RUNNING);
        if (claimed == 0) {
            // Job was already claimed by another worker
            return;
        }

        InvoiceDocument document = documents.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", null, "Document not found"));

        byte[] bytes;
        try (InputStream in = storage.open(tenantId, document.getStorageKey())) {
            bytes = in.readAllBytes();
        } catch (java.io.IOException e) {
            failJob(job, "FILE_OPEN_FAILED", "Unable to open file");
            return;
        }

        List<PageText> pages;
        try {
            pages = extractor.extract(bytes, ocrEngine);
        } catch (FixnaException e) {
            failJob(job, e.getCode(), e.getMessage());
            return;
        }

        int nativePages = (int) pages.stream().filter(p -> !p.fromOcr()).count();
        int ocrPages = (int) pages.stream().filter(PageText::fromOcr).count();

        InvoiceDocumentExtraction extraction = new InvoiceDocumentExtraction();
        extraction.setId(java.util.UUID.randomUUID());
        extraction.setTenantId(tenantId);
        extraction.setDocumentId(document.getId());
        extraction.setStatus(InvoiceExtractionStatus.NEEDS_REVIEW);
        extraction.setMethodNativePages(nativePages);
        extraction.setMethodOcrPages(ocrPages);
        extraction.setExtractorVersion(PdfTextExtractor.EXTRACTOR_VERSION);
        extractions.save(extraction);

        for (Draft draft : FieldExtractor.extract(pages)) {
            InvoiceDocumentField field = new InvoiceDocumentField();
            field.setId(java.util.UUID.randomUUID());
            field.setTenantId(tenantId);
            field.setExtractionId(extraction.getId());
            field.setFieldKey(draft.fieldKey());
            field.setFieldValue(draft.value());
            field.setMoneyAmount(draft.moneyAmount());
            field.setCurrency(draft.currency());
            field.setConfidence(BigDecimal.valueOf(draft.confidence()));
            field.setSource(draft.source());
            field.setPage(draft.page());
            field.setRawSnippet(draft.snippet());
            field.setExtractorVersion(PdfTextExtractor.EXTRACTOR_VERSION);
            fields.save(field);
        }

        document.setStatus(InvoiceDocumentStatus.NEEDS_REVIEW);
        document.setPurgeAfter(OffsetDateTime.now().plusDays(InvoiceDocumentService.RETENTION_DAYS));
        documents.save(document);

        int completed = jobRepository.completeJob(job.getId(), OffsetDateTime.now(),
                InvoiceDocumentJobStatus.RUNNING, InvoiceDocumentJobStatus.COMPLETED);
        if (completed == 0) {
            // Job was already completed/failed by another process (shouldn't happen)
            return;
        }

        audit.publish(new AuditEvent(
                "invoice.document.extracted",
                tenantId,
                null, // System user
                "invoice_document_extraction",
                extraction.getId().toString(),
                java.util.Map.of("documentId", document.getId().toString(),
                        "fields", String.valueOf(FieldExtractor.extract(pages).size())),
                null));
    }

    private void failJob(InvoiceDocumentJob job, String errorCode, String errorMessage) {
        int failed = jobRepository.failJob(job.getId(), OffsetDateTime.now(),
                InvoiceDocumentJobStatus.RUNNING, InvoiceDocumentJobStatus.FAILED,
                errorCode, errorMessage);
        if (failed == 0) {
            // Job might have been completed already
            return;
        }
        // Update document status to indicate extraction failed
        documents.findByIdAndTenantId(job.getDocumentId(), job.getTenantId())
                .ifPresent(doc -> {
                    doc.setStatus(InvoiceDocumentStatus.EXTRACTION_FAILED);
                    documents.save(doc);
                });
    }
}