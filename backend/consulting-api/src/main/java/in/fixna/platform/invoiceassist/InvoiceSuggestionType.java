package in.fixna.platform.invoiceassist;

/** Kinds of advisory suggestions generated for an invoice. */
public enum InvoiceSuggestionType {
    /** Invoice has no line items yet (deterministic). */
    MISSING_LINES,
    /** Recommended due date for a draft without one; applyable (deterministic). */
    DUE_DATE_RECOMMENDED,
    /** Invoice is past its due date (deterministic, informational). */
    OVERDUE_REMINDER,
    /** AI line-item ideas; advisory only, never auto-applied. */
    LINE_IDEAS
}
