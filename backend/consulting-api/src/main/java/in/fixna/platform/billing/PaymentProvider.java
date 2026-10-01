package in.fixna.platform.billing;

import java.math.BigDecimal;

public interface PaymentProvider {

    String providerId();

    void validatePaymentAmount(BigDecimal amount, BigDecimal remainingBalance);
}
