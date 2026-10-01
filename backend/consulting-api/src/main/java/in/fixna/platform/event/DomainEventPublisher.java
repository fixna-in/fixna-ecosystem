package in.fixna.platform.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
