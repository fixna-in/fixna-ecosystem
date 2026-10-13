package in.fixna.websiteaudit.audit;

public interface AuditPublisher {

    void publish(AuditEvent event);
}