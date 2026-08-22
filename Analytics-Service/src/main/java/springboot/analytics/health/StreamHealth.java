package springboot.analytics.health;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/** Whether the service is attached to the event stream, so empty totals are not read as "never borrowed". */
@Component
@RequiredArgsConstructor
public class StreamHealth {

    private final KafkaListenerEndpointRegistry registry;

    private final AtomicReference<Instant> lastEvent = new AtomicReference<>();

    /** Called for every event consumed, so "connected but silent" stays distinguishable. */
    public void recordEvent() {
        lastEvent.set(Instant.now());
    }

    /** When the last event arrived, or null if none has yet. */
    public Instant lastEventAt() {
        return lastEvent.get();
    }

    /** True when a listener holds a partition assignment; false while starting up or if the topic is absent. */
    public boolean connected() {
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            if (container.isRunning()) {
                var assigned = container.getAssignedPartitions();
                if (assigned != null && !assigned.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
