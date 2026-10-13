package in.fixna.platform.invoiceassist;

/** Lifecycle of an uploaded invoice document (V13, ADR-011). */
public enum InvoiceDocumentStatus {
    RECEIVED,
    QUEUED_FOR_EXTRACTION,
    NEEDS_REVIEW,
    APPROVED,
    POSTED,
    REJECTED,
    EXTRACTION_FAILED,
    DELETED;

    public boolean isTerminal() {
        return this == POSTED || this == REJECTED || this == DELETED || this == EXTRACTION_FAILED;
    }
}