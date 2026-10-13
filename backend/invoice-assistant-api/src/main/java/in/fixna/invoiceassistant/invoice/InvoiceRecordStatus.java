package in.fixna.invoiceassistant.invoice;

/** Lifecycle of an owned invoice record (standalone V2). */
public enum InvoiceRecordStatus {
    DRAFT,
    NEEDS_REVIEW,
    APPROVED,
    POSTED,
    VOID;

    /** Only editable states can receive posted extractions. */
    public boolean isEditable() {
        return this == DRAFT || this == NEEDS_REVIEW;
    }
}
