package in.fixna.invoiceassistant.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import in.fixna.invoiceassistant.membership.TenantMembershipRepository;

@Component
public class LoggingAuditPublisher implements AuditPublisher {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.audit");

    private final AuditLogRepository logs;

    public LoggingAuditPublisher(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Override
    public void publish(AuditEvent event) {
        LOG.info(
                "operation=audit tenantId={} userId={} action={} resource={}:{}",
                event.tenantId(),
                event.userId(),
                event.action(),
                event.resourceType(),
                event.resourceId());
        try {
            AuditLog row = new AuditLog();
            row.setId(UUID.randomUUID());
            row.setTenantId(event.tenantId());
            row.setUserId(event.userId());
            row.setAction(event.action());
            row.setResourceType(event.resourceType());
            row.setResourceId(event.resourceId());
            row.setCreatedAt(OffsetDateTime.now());
            logs.save(row);
        } catch (RuntimeException ex) {
            LOG.warn("operation=audit_persist_failed action={}", event.action(), ex);
        }
    }
}
