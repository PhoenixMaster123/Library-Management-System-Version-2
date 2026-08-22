package springboot.analytics.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import springboot.analytics.health.StreamHealth;
import springboot.analytics.service.LoanStatisticsService;

/** The service's only inbound path: it is fed by the topic, never told anything over HTTP. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoanEventListener {

    private final LoanStatisticsService statistics;
    private final StreamHealth streamHealth;

    @KafkaListener(
            topics = "${library.events.topic:library.loans}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service}")
    /** Records one loan event: marks the stream alive, then folds it into the totals. */
    public void onLoanEvent(LoanEvent event) {
        log.info("Received {} for '{}'", event.type(), event.bookTitle());
        streamHealth.recordEvent();
        statistics.record(event);
    }
}
