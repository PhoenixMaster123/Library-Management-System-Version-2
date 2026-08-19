package springboot.analytics.health;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Whether this service is actually attached to the event stream.
 *
 * <p>Without this the statistics are ambiguous in a way that matters: an empty projection looks
 * identical whether nothing has ever been borrowed or the broker has been unreachable the whole
 * time. The first is a fact about the library; the second is a fact about the plumbing, and
 * reporting it as the first tells the reader something untrue.
 *
 * <p>Connectivity is read from the listener container's partition assignments rather than by
 * pinging the broker: a consumer holding assignments is by definition talking to one, and the
 * answer costs no I/O.
 */
@Component
@RequiredArgsConstructor
public class StreamHealth {

    private final KafkaListenerEndpointRegistry registry;

    private final AtomicReference<Instant> lastEvent = new AtomicReference<>();

    /** Called for every event consumed, so "connected but silent" stays distinguishable. */
    public void recordEvent() {
        lastEvent.set(Instant.now());
    }

    public Instant lastEventAt() {
        return lastEvent.get();
    }

    /**
     * True when at least one listener container holds a partition assignment.
     *
     * <p>Note this is false for a short window after start-up, before the group has rebalanced,
     * and false when the broker is up but the topic does not exist yet - in both cases no event
     * can arrive, which is exactly what the caller is asking about.
     */
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
