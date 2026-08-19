package app.adapters.output.events;

import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.port.output.LoanEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes loan events to Kafka, keyed by book id so one book's events stay in order.
 *
 * <p>Fire-and-forget: a broker that is down must not stop anyone borrowing.
 */
@Component
@Slf4j
public class LoanEventKafkaPublisher implements LoanEventPort {

    private final KafkaTemplate<String, LoanEvent> kafkaTemplate;
    private final String topic;
    private final boolean enabled;

    public LoanEventKafkaPublisher(KafkaTemplate<String, LoanEvent> kafkaTemplate,
                                   @Value("${library.events.topic:library.loans}") String topic,
                                   @Value("${library.events.enabled:true}") boolean enabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.enabled = enabled;
    }

    @Override
    public void bookBorrowed(Customer customer, Book book) {
        publish(LoanEvent.BORROWED, customer, book);
    }

    @Override
    public void bookReturned(Customer customer, Book book) {
        publish(LoanEvent.RETURNED, customer, book);
    }

    private void publish(String type, Customer customer, Book book) {
        if (!enabled || customer == null || book == null) {
            return;
        }

        LoanEvent event = new LoanEvent(
                type,
                customer.getCustomerId(),
                customer.getName(),
                book.getBookId(),
                book.getTitle(),
                book.getIsbn(),
                Instant.now());

        try {
            kafkaTemplate.send(topic, String.valueOf(book.getBookId()), event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Could not publish {} for book {}: {}", type, book.getTitle(), error.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Could not publish {} for book {}: {}", type, book.getTitle(), e.getMessage());
        }
    }
}
