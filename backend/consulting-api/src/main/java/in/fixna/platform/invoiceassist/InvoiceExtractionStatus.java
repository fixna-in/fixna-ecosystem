package in.fixna.platform.invoiceassist;

/** Review state of an extraction (V13, ADR-012). */
public enum InvoiceExtractionStatus {
    NEEDS_REVIEW,
    APPROVED,
    REJECTED,
    POSTED;

    public boolean isTerminal() {
        return this == REJECTED || this == POSTED;
    }
}