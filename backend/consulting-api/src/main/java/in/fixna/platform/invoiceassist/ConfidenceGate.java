package in.fixna.platform.invoiceassist;

import java.math.BigDecimal;

/**
 * Deterministic confidence gates for extracted fields (ADR-012).
 * Thresholds are identical everywhere so behavior is fully testable:
 * <ul>
 *   <li>≥ 0.90 — auto-prefill candidate (still shown in review)</li>
 *   <li>0.70 – 0.89 — requires explicit human confirmation</li>
 *   <li>&lt; 0.70 — hidden by default</li>
 * </ul>
 * A monetary field additionally needs an exact {@link BigDecimal} parse
 * (scale ≤ 2); a failed parse rejects the field regardless of confidence.
 */
final class ConfidenceGate {

    static final BigDecimal AUTOFILL = new BigDecimal("0.90");
    static final BigDecimal REVIEW = new BigDecimal("0.70");

    private ConfidenceGate() {}

    enum Disposition {
        AUTOFILL,
        NEEDS_REVIEW,
        HIDDEN
    }

    static Disposition disposition(BigDecimal confidence, boolean monetaryParsed) {
        if (!monetaryParsed) {
            return Disposition.HIDDEN;
        }
        if (confidence.compareTo(AUTOFILL) >= 0) {
            return Disposition.AUTOFILL;
        }
        if (confidence.compareTo(REVIEW) >= 0) {
            return Disposition.NEEDS_REVIEW;
        }
        return Disposition.HIDDEN;
    }

    /** True when the field may be shown in the default review view. */
    static boolean isVisible(BigDecimal confidence, boolean monetaryParsed) {
        return disposition(confidence, monetaryParsed) != Disposition.HIDDEN;
    }

    /**
     * True when approval is blocked until a human confirms the field:
     * anything below autofill that is still shown must be confirmed.
     */
    static boolean blocksApprovalUntilConfirmed(BigDecimal confidence, boolean monetaryParsed,
            boolean confirmed) {
        if (!monetaryParsed) {
            return false; // hidden fields do not block; they are excluded from the review set
        }
        if (confidence.compareTo(AUTOFILL) >= 0) {
            return false;
        }
        return !confirmed;
    }
}