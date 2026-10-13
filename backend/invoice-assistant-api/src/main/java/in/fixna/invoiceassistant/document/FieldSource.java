package in.fixna.invoiceassistant.document;

/** Provenance source for an extracted field (ADR-011/012). */
public enum FieldSource {
    NATIVE_TEXT,
    OCR,
    AI_ADVISORY,
    MANUAL;

    /** AI-sourced values can never satisfy a confidence gate alone. */
    public boolean isAdvisoryOnly() {
        return this == AI_ADVISORY;
    }
}
