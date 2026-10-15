package in.fixna.invoiceassistant.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ConfidenceGateTest {

    @Test
    void autofillAtOrAboveNinety() {
        assertThat(ConfidenceGate.disposition(new BigDecimal("0.95"), true))
                .isEqualTo(ConfidenceGate.Disposition.AUTOFILL);
        assertThat(ConfidenceGate.disposition(new BigDecimal("0.90"), true))
                .isEqualTo(ConfidenceGate.Disposition.AUTOFILL);
    }

    @Test
    void reviewBandBetweenSeventyAndNinety() {
        assertThat(ConfidenceGate.disposition(new BigDecimal("0.85"), true))
                .isEqualTo(ConfidenceGate.Disposition.NEEDS_REVIEW);
        assertThat(ConfidenceGate.disposition(new BigDecimal("0.70"), true))
                .isEqualTo(ConfidenceGate.Disposition.NEEDS_REVIEW);
    }

    @Test
    void hiddenBelowSeventyOrUnparsed() {
        assertThat(ConfidenceGate.disposition(new BigDecimal("0.69"), true))
                .isEqualTo(ConfidenceGate.Disposition.HIDDEN);
        assertThat(ConfidenceGate.disposition(new BigDecimal("0.99"), false))
                .isEqualTo(ConfidenceGate.Disposition.HIDDEN);
    }

    @Test
    void approvalBlockedUntilLowConfidenceConfirmed() {
        BigDecimal review = new BigDecimal("0.85");
        assertThat(ConfidenceGate.blocksApprovalUntilConfirmed(review, true, false)).isTrue();
        assertThat(ConfidenceGate.blocksApprovalUntilConfirmed(review, true, true)).isFalse();
        assertThat(ConfidenceGate.blocksApprovalUntilConfirmed(new BigDecimal("0.95"), true, false))
                .isFalse();
        assertThat(ConfidenceGate.blocksApprovalUntilConfirmed(new BigDecimal("0.50"), true, false))
                .isFalse(); // hidden fields never block
    }
}
