package in.fixna.platform.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface DomainEvent {

    String eventName();

    UUID tenantId();

    OffsetDateTime occurredAt();
}
