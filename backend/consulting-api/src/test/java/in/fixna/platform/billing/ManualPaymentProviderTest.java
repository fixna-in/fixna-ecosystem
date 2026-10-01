package in.fixna.platform.billing;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualPaymentProviderTest {

    private final ManualPaymentProvider provider = new ManualPaymentProvider();

    @Test
    void acceptsValidPaymentAmount() {
        assertThatCode(() -> provider.validatePaymentAmount(new BigDecimal("50.00"), new BigDecimal("100.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroOrNegativeAmount() {
        assertThatThrownBy(() -> provider.validatePaymentAmount(BigDecimal.ZERO, new BigDecimal("100.00")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_PAYMENT_AMOUNT");

        assertThatThrownBy(() -> provider.validatePaymentAmount(new BigDecimal("-1.00"), new BigDecimal("100.00")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_PAYMENT_AMOUNT");
    }

    @Test
    void rejectsAmountExceedingRemainingBalance() {
        assertThatThrownBy(() -> provider.validatePaymentAmount(new BigDecimal("150.00"), new BigDecimal("100.00")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("PAYMENT_EXCEEDS_BALANCE");
    }
}
