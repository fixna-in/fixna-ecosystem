package in.fixna.platform.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class PersistedAuditPublisher implements AuditPublisher {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.audit");

    private final AuditService auditService;
    private final LoggingAuditPublisher logging;

    public PersistedAuditPublisher(AuditService auditService, LoggingAuditPublisher logging) {
        this.auditService = auditService;
        this.logging = logging;
    }

    @Override
    public void publish(AuditEvent event) {
        logging.publish(event);
        Runnable write = () -> {
            try {
                auditService.record(
                        event.tenantId(),
                        event.actorUserId(),
                        event.action(),
                        event.entityType(),
                        event.entityId(),
                        event.details(),
                        event.occurredAt());
            } catch (RuntimeException ex) {
                LOG.warn("Audit persist failed for action {}", event.action());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write.run();
                }
            });
        } else {
            write.run();
        }
    }
}
