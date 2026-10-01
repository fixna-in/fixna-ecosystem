package in.fixna.platform.billing;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.common.web.FixnaException;

@Component
public class ManualPaymentProvider implements PaymentProvider {

    @Override
    public String providerId() {
        return "manual";
    }

    @Override
    public void validatePaymentAmount(BigDecimal amount, BigDecimal remainingBalance) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new FixnaException(
                    "INVALID_PAYMENT_AMOUNT",
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must be greater than zero");
        }
        if (remainingBalance == null || amount.compareTo(remainingBalance) > 0) {
            throw new FixnaException(
                    "PAYMENT_EXCEEDS_BALANCE",
                    HttpStatus.BAD_REQUEST,
                    "Payment amount exceeds remaining invoice balance");
        }
    }
}
