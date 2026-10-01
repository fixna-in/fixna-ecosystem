package in.fixna.platform.consulting.invoice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.invoice.Payment;

public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String reference,
        OffsetDateTime paidAt,
        UUID recordedByUserId,
        OffsetDateTime createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getReference(),
                payment.getPaidAt(),
                payment.getRecordedByUserId(),
                payment.getCreatedAt());
    }
}
