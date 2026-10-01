package in.fixna.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationProvider implements NotificationProvider {

    private static final Logger NOTIFY_LOG = LoggerFactory.getLogger("fixna.notify");

    @Override
    public String channel() {
        return "LOG";
    }

    @Override
    public void send(Notification notification) {
        NOTIFY_LOG.info(
                "event={} tenant={} entity={}:{} details={}",
                notification.event(),
                notification.tenantId(),
                notification.entityType(),
                notification.entityId(),
                notification.details());
    }
}
