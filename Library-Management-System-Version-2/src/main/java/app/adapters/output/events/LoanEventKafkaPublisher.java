package app.adapters.output.events;

import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.port.output.LoanEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Publishes loan events to Kafka, keyed by book id so one book's events stay in order. */
@Component
@Slf4j
public class LoanEventKafkaPublisher implements LoanEventPort {

    private final KafkaTemplate<String, LoanEvent> kafkaTemplate;
    private final String topic;
    private final boolean enabled;

    /** Reads the topic and the on/off switch from configuration. */
    public LoanEventKafkaPublisher(KafkaTemplate<String, LoanEvent> kafkaTemplate,
                                   @Value("${library.events.topic:library.loans}") String topic,
                                   @Value("${library.events.enabled:true}") boolean enabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.enabled = enabled;
    }

    /** Announces a borrow. */
    @Override
    public void bookBorrowed(Customer customer, Book book) {
        publish(LoanEvent.BORROWED, customer, book);
    }

    /** Announces a return. */
    @Override
    public void bookReturned(Customer customer, Book book) {
        publish(LoanEvent.RETURNED, customer, book);
    }

    /** Sends one event and forgets it: a broker that is down must not stop a borrow. */
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
