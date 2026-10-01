package in.fixna.platform.audit;

public interface AuditPublisher {

    void publish(AuditEvent event);
}
