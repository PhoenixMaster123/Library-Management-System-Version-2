package springboot.analytics;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import springboot.analytics.event.LoanEvent;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Reading what the library actually puts on the topic.
 *
 * <p>{@link LoanStatisticsServiceTest} hands events to the service directly, so it never exercises
 * deserialization - and that is where the two services can disagree without any test noticing.
 */
class LoanEventDeserializationTest {

    /** A borrow as the library's JsonSerializer writes it. */
    private static final String PUBLISHED_JSON = """
            {"type":"BOOK_BORROWED",
             "customerId":"3f1a5f6e-7c2b-4a91-9d3e-5b8c1a2d4e6f",
             "customerName":"Ada Lovelace",
             "bookId":"9c8b7a6d-5e4f-4321-8a9b-0c1d2e3f4a5b",
             "bookTitle":"Dune",
             "bookIsbn":"978-0-441-01359-3",
             "occurredAt":"2026-08-19T10:15:30Z"}""";

    private static JsonDeserializer<LoanEvent> configuredAsTheConsumerIs() {
        JsonDeserializer<LoanEvent> deserializer = new JsonDeserializer<>(LoanEvent.class);
        deserializer.setUseTypeHeaders(false);
        return deserializer;
    }

    @Test
    void readsTheEventTheLibraryPublishes() {
        try (JsonDeserializer<LoanEvent> deserializer = configuredAsTheConsumerIs()) {
            LoanEvent event = deserializer.deserialize(
                    "library.loans", PUBLISHED_JSON.getBytes(StandardCharsets.UTF_8));

            assertThat(event.type()).isEqualTo(LoanEvent.BORROWED);
            assertThat(event.bookTitle()).isEqualTo("Dune");
            assertThat(event.customerName()).isEqualTo("Ada Lovelace");
            assertThat(event.occurredAt()).isNotNull();
        }
    }

    /**
     * Regression: the producer used to stamp __TypeId__ with its own class name, and honouring it
     * threw ClassNotFoundException here - which wedged the consumer on the offending offset and
     * stopped every later event. Only a running broker showed it; this makes it a unit test.
     */
    @Test
    void ignoresATypeHeaderNamingAClassThisServiceDoesNotHave() {
        Headers headers = new RecordHeaders();
        headers.add("__TypeId__", "app.adapters.output.events.LoanEvent".getBytes(StandardCharsets.UTF_8));

        try (JsonDeserializer<LoanEvent> deserializer = configuredAsTheConsumerIs()) {
            assertThatCode(() -> {
                LoanEvent event = deserializer.deserialize(
                        "library.loans", headers, PUBLISHED_JSON.getBytes(StandardCharsets.UTF_8));
                assertThat(event.bookTitle()).isEqualTo("Dune");
            }).doesNotThrowAnyException();
        }
    }

    /** Unknown fields must not break the consumer when the library adds one. */
    @Test
    void toleratesAFieldItDoesNotKnow() {
        String withExtra = PUBLISHED_JSON.replace("\"type\":", "\"somethingNew\":\"x\",\"type\":");

        try (JsonDeserializer<LoanEvent> deserializer = configuredAsTheConsumerIs()) {
            assertThatCode(() -> deserializer.deserialize(
                    "library.loans", withExtra.getBytes(StandardCharsets.UTF_8)))
                    .doesNotThrowAnyException();
        }
    }
}
