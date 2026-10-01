package in.fixna.platform.common.observability;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppReadiness implements ReadinessController.RunnableIsReady {

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        ready.set(true);
    }

    @Override
    public boolean get() {
        return ready.get();
    }
}
