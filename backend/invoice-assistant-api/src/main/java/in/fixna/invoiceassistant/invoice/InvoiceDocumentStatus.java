package in.fixna.invoiceassistant.invoice;

/** Lifecycle of an uploaded invoice document (standalone V3, ADR-011). */
public enum InvoiceDocumentStatus {
    RECEIVED,
    NEEDS_REVIEW,
    APPROVED,
    POSTED,
    REJECTED,
    DELETED;

    public boolean isTerminal() {
        return this == POSTED || this == REJECTED || this == DELETED;
    }
}
