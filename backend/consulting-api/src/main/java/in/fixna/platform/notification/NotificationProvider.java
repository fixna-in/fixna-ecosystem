package in.fixna.platform.notification;

import java.util.Map;
import java.util.UUID;

public interface NotificationProvider {

    String channel();

    void send(Notification notification);

    record Notification(String event, UUID tenantId, String entityType, String entityId,
            Map<String, String> details) {

        public Notification {
            if (event == null || event.isBlank()) {
                throw new IllegalArgumentException("Notification event is required");
            }
            details = details == null ? Map.of() : Map.copyOf(details);
        }
    }
}
