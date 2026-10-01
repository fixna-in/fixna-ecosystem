package in.fixna.platform.consulting.invoice.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.event.DomainEvent;

public record PaymentRecorded(
        UUID paymentId, UUID invoiceId, UUID tenantId, BigDecimal amount, OffsetDateTime occurredAt)
        implements DomainEvent {

    public PaymentRecorded(UUID paymentId, UUID invoiceId, UUID tenantId, BigDecimal amount) {
        this(paymentId, invoiceId, tenantId, amount, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "payment.recorded";
    }

    @Override
    public UUID tenantId() {
        return tenantId;
    }

    @Override
    public OffsetDateTime occurredAt() {
        return occurredAt;
    }
}
