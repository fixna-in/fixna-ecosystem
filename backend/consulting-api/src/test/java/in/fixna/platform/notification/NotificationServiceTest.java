package in.fixna.platform.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    @Test
    void dispatchesToAllProviders() {
        NotificationProvider provider = mock(NotificationProvider.class);
        NotificationService service = new NotificationService(List.of(provider));
        UUID tenantId = UUID.randomUUID();

        service.notify("client.created", tenantId, "client", "abc", Map.of("name", "Acme"));

        verify(provider).send(new NotificationProvider.Notification(
                "client.created", tenantId, "client", "abc", Map.of("name", "Acme")));
    }
}
