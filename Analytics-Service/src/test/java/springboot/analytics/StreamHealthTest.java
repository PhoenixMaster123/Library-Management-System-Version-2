package springboot.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.apache.kafka.common.TopicPartition;
import springboot.analytics.health.StreamHealth;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The signal that keeps an empty projection honest: "no events have arrived" must be
 * distinguishable from "nothing has ever been borrowed".
 */
class StreamHealthTest {

    private static StreamHealth withContainer(boolean running, Set<TopicPartition> assigned) {
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        when(container.isRunning()).thenReturn(running);
        if (running) {
            when(container.getAssignedPartitions()).thenReturn(assigned);
        }

        KafkaListenerEndpointRegistry registry = mock(KafkaListenerEndpointRegistry.class);
        when(registry.getListenerContainers()).thenReturn(List.of(container));

        return new StreamHealth(registry);
    }

    @Test
    void isConnectedWhenTheConsumerHoldsAnAssignment() {
        StreamHealth health = withContainer(true, Set.of(new TopicPartition("library.loans", 0)));

        assertThat(health.connected()).isTrue();
    }

    /** The case that caused the trouble: service up, broker unreachable, so no assignment. */
    @Test
    void isNotConnectedWithoutAnAssignment() {
        StreamHealth health = withContainer(true, Set.of());

        assertThat(health.connected()).isFalse();
    }

    @Test
    void isNotConnectedWhenTheContainerIsStopped() {
        StreamHealth health = withContainer(false, Set.of(new TopicPartition("library.loans", 0)));

        assertThat(health.connected()).isFalse();
    }

    @Test
    void isNotConnectedWithNoContainersAtAll() {
        KafkaListenerEndpointRegistry registry = mock(KafkaListenerEndpointRegistry.class);
        when(registry.getListenerContainers()).thenReturn(List.of());

        assertThat(new StreamHealth(registry).connected()).isFalse();
    }

    @Test
    void reportsNoLastEventUntilOneArrives() {
        StreamHealth health = withContainer(true, Set.of());

        assertThat(health.lastEventAt()).isNull();

        health.recordEvent();

        assertThat(health.lastEventAt()).isNotNull();
    }
}
