package in.fixna.invoiceassistant.invoice;

/** Review state of an extraction (standalone V3, ADR-012). */
public enum InvoiceExtractionStatus {
    NEEDS_REVIEW,
    APPROVED,
    REJECTED,
    POSTED;

    public boolean isTerminal() {
        return this == REJECTED || this == POSTED;
    }
}
