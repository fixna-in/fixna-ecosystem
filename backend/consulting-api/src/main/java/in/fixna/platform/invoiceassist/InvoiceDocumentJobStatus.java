package in.fixna.platform.invoiceassist;

/**
 * Lifecycle of an asynchronous invoice document extraction job.
 */
public enum InvoiceDocumentJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}