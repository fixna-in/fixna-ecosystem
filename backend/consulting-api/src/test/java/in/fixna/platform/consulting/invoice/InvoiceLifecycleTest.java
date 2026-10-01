package in.fixna.platform.consulting.invoice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceLifecycleTest {

    @Test
    void draftAllowsSendAndCancel() {
        assertThatCode(() -> InvoiceStatus.DRAFT.validateTransitionTo(InvoiceStatus.SENT))
                .doesNotThrowAnyException();
        assertThatCode(() -> InvoiceStatus.DRAFT.validateTransitionTo(InvoiceStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> InvoiceStatus.DRAFT.validateTransitionTo(InvoiceStatus.PAID))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void sentAllowsPartialFullPaymentAndCancel() {
        assertThatCode(() -> InvoiceStatus.SENT.validateTransitionTo(InvoiceStatus.PARTIALLY_PAID))
                .doesNotThrowAnyException();
        assertThatCode(() -> InvoiceStatus.SENT.validateTransitionTo(InvoiceStatus.PAID))
                .doesNotThrowAnyException();
        assertThatCode(() -> InvoiceStatus.SENT.validateTransitionTo(InvoiceStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> InvoiceStatus.SENT.validateTransitionTo(InvoiceStatus.DRAFT))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void partiallyPaidAllowsPaidAndCancel() {
        assertThatCode(() -> InvoiceStatus.PARTIALLY_PAID.validateTransitionTo(InvoiceStatus.PAID))
                .doesNotThrowAnyException();
        assertThatCode(() -> InvoiceStatus.PARTIALLY_PAID.validateTransitionTo(InvoiceStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> InvoiceStatus.PARTIALLY_PAID.validateTransitionTo(InvoiceStatus.SENT))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void terminalStatesRejectTransitions() {
        assertThatThrownBy(() -> InvoiceStatus.PAID.validateTransitionTo(InvoiceStatus.SENT))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> InvoiceStatus.CANCELLED.validateTransitionTo(InvoiceStatus.DRAFT))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }
}
