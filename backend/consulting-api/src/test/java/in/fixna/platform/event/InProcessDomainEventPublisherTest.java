package in.fixna.platform.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InProcessDomainEventPublisherTest {

    @Test
    void invokesMatchingHandlers() {
        AtomicBoolean handled = new AtomicBoolean(false);
        InProcessDomainEventPublisher.DomainEventHandler handler = new InProcessDomainEventPublisher.DomainEventHandler() {
            @Override
            public boolean supports(DomainEvent event) {
                return "test.event".equals(event.eventName());
            }

            @Override
            public void handle(DomainEvent event) {
                handled.set(true);
            }
        };
        InProcessDomainEventPublisher publisher = new InProcessDomainEventPublisher(List.of(handler));
        DomainEvent event = new DomainEvent() {
            @Override
            public String eventName() {
                return "test.event";
            }

            @Override
            public UUID tenantId() {
                return UUID.randomUUID();
            }

            @Override
            public OffsetDateTime occurredAt() {
                return OffsetDateTime.now();
            }
        };

        publisher.publish(event);

        assertThat(handled).isTrue();
    }
}
