package in.fixna.platform.consulting.invoice.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.event.DomainEvent;

public record InvoiceSent(UUID invoiceId, UUID tenantId, OffsetDateTime occurredAt) implements DomainEvent {

    public InvoiceSent(UUID invoiceId, UUID tenantId) {
        this(invoiceId, tenantId, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "invoice.sent";
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
