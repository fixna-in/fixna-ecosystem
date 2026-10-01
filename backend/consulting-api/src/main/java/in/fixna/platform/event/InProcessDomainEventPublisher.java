package in.fixna.platform.event;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InProcessDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger("fixna.events");

    private final List<DomainEventHandler> handlers;

    public InProcessDomainEventPublisher(List<DomainEventHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void publish(DomainEvent event) {
        LOG.info("event={} tenant={} at={}", event.eventName(), event.tenantId(), event.occurredAt());
        handlers.stream()
                .filter(handler -> handler.supports(event))
                .forEach(handler -> handler.handle(event));
    }

    public interface DomainEventHandler {
        boolean supports(DomainEvent event);

        void handle(DomainEvent event);
    }
}
