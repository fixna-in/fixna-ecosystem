package in.fixna.platform.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final List<NotificationProvider> providers;

    public NotificationService(List<NotificationProvider> providers) {
        this.providers = providers;
    }

    public void notify(String event, UUID tenantId, String entityType, String entityId, Map<String, String> details) {
        NotificationProvider.Notification notification =
                new NotificationProvider.Notification(event, tenantId, entityType, entityId, details);
        providers.forEach(provider -> provider.send(notification));
    }
}
