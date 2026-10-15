package in.fixna.invoiceassistant.invoice;

/**
 * Lifecycle of an asynchronous invoice document extraction job.
 */
public enum InvoiceDocumentJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
