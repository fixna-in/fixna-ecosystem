package in.fixna.invoiceassistant.audit;

public interface AuditPublisher {
    void publish(AuditEvent event);
}
