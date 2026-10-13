package in.fixna.invoiceassistant.invoice;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import in.fixna.invoiceassistant.audit.AuditEvent;
import in.fixna.invoiceassistant.audit.AuditPublisher;
import in.fixna.invoiceassistant.document.FieldExtractor;
import in.fixna.invoiceassistant.document.FieldSource;
import in.fixna.invoiceassistant.document.OcrEngine;
import in.fixna.invoiceassistant.document.PdfTextExtractor;
import in.fixna.invoiceassistant.document.PdfTextExtractor.PageText;
import in.fixna.invoiceassistant.file.FileStorage;
import in.fixna.platform.common.web.FixnaException;

/**
 * Worker that processes queued invoice document extraction jobs.
 * Runs on a fixed poll (default 10s, configurable via
 * {@code fixna.invoice.job-worker.poll-ms}). Each job is claimed with an
 * atomic QUEUED → RUNNING transition so concurrent workers never double-process.
 */
@Component
public class InvoiceDocumentJobWorker {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.invoice.job.worker");
    private static final int BATCH_SIZE = 5;

    private final InvoiceDocumentJobRepository jobRepository;
    private final InvoiceDocumentRepository documents;
    private final InvoiceDocumentExtractionRepository extractions;
    private final InvoiceDocumentFieldRepository fields;
    private final FileStorage storage;
    private final PdfTextExtractor extractor;
    private final OcrEngine ocrEngine;
    private final AuditPublisher audit;
    private final TransactionTemplate tx;

    public InvoiceDocumentJobWorker(
            InvoiceDocumentJobRepository jobRepository,
            InvoiceDocumentRepository documents,
            InvoiceDocumentExtractionRepository extractions,
            InvoiceDocumentFieldRepository fields,
            FileStorage storage,
            PdfTextExtractor extractor,
            OcrEngine ocrEngine,
            AuditPublisher audit,
            PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.documents = documents;
        this.extractions = extractions;
        this.fields = fields;
        this.storage = storage;
        this.extractor = extractor;
        this.ocrEngine = ocrEngine;
        this.audit = audit;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedRateString = "${fixna.invoice.job-worker.poll-ms:10000}")
    public void poll() {
        List<InvoiceDocumentJob> jobs = jobRepository
                .findQueuedJobs(InvoiceDocumentJobStatus.QUEUED)
                .stream()
                .limit(BATCH_SIZE)
                .toList();
        for (InvoiceDocumentJob job : jobs) {
            tx.executeWithoutResult(status -> process(job));
        }
    }

    private void process(InvoiceDocumentJob job) {
        OffsetDateTime now = OffsetDateTime.now();
        int claimed = jobRepository.claimJob(
                job.getId(), now,
                InvoiceDocumentJobStatus.QUEUED, InvoiceDocumentJobStatus.RUNNING);
        if (claimed == 0) {
            return; // another worker won the claim
        }
        UUID tenantId = job.getTenantId();
        InvoiceDocument document = documents.findByIdAndTenantId(job.getDocumentId(), tenantId)
                .orElse(null);
        if (document == null || document.getStorageKey() == null) {
            failJob(job, "DOCUMENT_UNAVAILABLE", "Document or stored bytes not found");
            return;
        }

        byte[] bytes;
        try (InputStream in = storage.open(tenantId, document.getStorageKey())) {
            bytes = in.readAllBytes();
        } catch (Exception e) {
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
        extraction.setId(UUID.randomUUID());
        extraction.setTenantId(tenantId);
        extraction.setDocumentId(document.getId());
        extraction.setStatus(InvoiceExtractionStatus.NEEDS_REVIEW);
        extraction.setMethodNativePages(nativePages);
        extraction.setMethodOcrPages(ocrPages);
        extraction.setExtractorVersion(PdfTextExtractor.EXTRACTOR_VERSION);
        extractions.save(extraction);

        List<FieldExtractor.Draft> drafts = FieldExtractor.extract(pages);
        for (FieldExtractor.Draft draft : drafts) {
            InvoiceDocumentField field = new InvoiceDocumentField();
            field.setId(UUID.randomUUID());
            field.setTenantId(tenantId);
            field.setExtractionId(extraction.getId());
            field.setFieldKey(draft.fieldKey());
            field.setFieldValue(draft.value());
            field.setMoneyAmount(draft.moneyAmount());
            field.setCurrency(draft.currency());
            field.setConfidence(BigDecimal.valueOf(draft.confidence()));
            field.setSource(draft.source() == null ? FieldSource.NATIVE_TEXT : draft.source());
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
            return;
        }

        audit.publish(new AuditEvent(
                "invoice.document.extracted",
                tenantId,
                null, // system actor
                "invoice_document_extraction",
                extraction.getId().toString(),
                Map.of("documentId", document.getId().toString(),
                        "fields", String.valueOf(drafts.size()))));
    }

    private void failJob(InvoiceDocumentJob job, String errorCode, String errorMessage) {
        int failed = jobRepository.failJob(job.getId(), OffsetDateTime.now(),
                InvoiceDocumentJobStatus.RUNNING, InvoiceDocumentJobStatus.FAILED,
                errorCode, errorMessage);
        if (failed == 0) {
            return;
        }
        LOG.warn("operation=invoice_extraction status=failed jobId={} code={}",
                job.getId(), errorCode);
        audit.publish(new AuditEvent(
                "invoice.document.extraction_failed",
                job.getTenantId(),
                null,
                "invoice_document_job",
                job.getId().toString(),
                Map.of("documentId", job.getDocumentId().toString(),
                        "errorCode", errorCode)));
    }
}
