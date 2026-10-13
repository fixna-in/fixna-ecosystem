package in.fixna.invoiceassistant.invoice;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.invoiceassistant.file.FileStorage;

/**
 * Retention purge (IA-09). Periodically hard-deletes documents whose
 * {@code purge_after} has passed: drops stored bytes, fields, and extraction
 * rows, leaving an id-only tombstone on the document row. Never runs inside a
 * request path; tombstone-only semantics keep the audit trail intact.
 */
@Component
public class InvoiceRetentionJob {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.invoice.retention");

    private final InvoiceDocumentRepository documents;
    private final InvoiceDocumentExtractionRepository extractions;
    private final InvoiceDocumentFieldRepository fields;
    private final FileStorage storage;

    public InvoiceRetentionJob(
            InvoiceDocumentRepository documents,
            InvoiceDocumentExtractionRepository extractions,
            InvoiceDocumentFieldRepository fields,
            FileStorage storage) {
        this.documents = documents;
        this.extractions = extractions;
        this.fields = fields;
        this.storage = storage;
    }

    @Scheduled(cron = "${fixna.invoice.retention-cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpired() {
        OffsetDateTime cutoff = OffsetDateTime.now();
        List<InvoiceDocument> expired = documents.findByPurgeAfterBeforeAndStatusNot(
                cutoff, InvoiceDocumentStatus.DELETED);
        int purged = 0;
        for (InvoiceDocument document : expired) {
            if (document.getStorageKey() != null) {
                try {
                    storage.delete(document.getTenantId(), document.getStorageKey());
                } catch (RuntimeException e) {
                    LOG.warn("operation=retention_purge status=storage_delete_failed documentId={}",
                            document.getId());
                    continue;
                }
            }
            List<InvoiceDocumentExtraction> docExtractions =
                    extractions.findByDocumentIdAndTenantIdOrderByCreatedAtDesc(
                            document.getId(), document.getTenantId());
            for (InvoiceDocumentExtraction extraction : docExtractions) {
                fields.deleteByExtractionId(extraction.getId());
                extractions.delete(extraction);
            }
            document.setStorageKey(null);
            document.setStatus(InvoiceDocumentStatus.DELETED);
            document.setPurgeAfter(null);
            documents.save(document);
            purged++;
        }
        if (purged > 0) {
            LOG.info("operation=retention_purge status=completed count={}", purged);
        }
    }
}
