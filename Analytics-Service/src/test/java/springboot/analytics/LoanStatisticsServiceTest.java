package springboot.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import springboot.analytics.event.LoanEvent;
import springboot.analytics.service.LoanStatisticsService;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the folding of events into totals without needing a broker. */
@SpringBootTest
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=false")
class LoanStatisticsServiceTest {

    @Autowired
    private LoanStatisticsService statistics;

    private LoanEvent event(String type, UUID bookId, String title) {
        return new LoanEvent(type, UUID.randomUUID(), "A Member", bookId, title, "123", Instant.now());
    }

    @Test
    void countsBorrowsAndReturnsPerBook() {
        UUID bookId = UUID.randomUUID();

        statistics.record(event(LoanEvent.BORROWED, bookId, "Dune"));
        statistics.record(event(LoanEvent.BORROWED, bookId, "Dune"));
        statistics.record(event(LoanEvent.RETURNED, bookId, "Dune"));

        var stat = statistics.mostBorrowed(10).stream()
                .filter(s -> s.getBookId().equals(bookId))
                .findFirst()
                .orElseThrow();

        assertThat(stat.getTimesBorrowed()).isEqualTo(2);
        assertThat(stat.getTimesReturned()).isEqualTo(1);
        assertThat(stat.getCurrentlyOut()).isEqualTo(1);
    }

    @Test
    void ranksTheMostBorrowedFirst() {
        UUID popular = UUID.randomUUID();
        UUID quiet = UUID.randomUUID();

        statistics.record(event(LoanEvent.BORROWED, quiet, "Quiet Book"));
        for (int i = 0; i < 5; i++) {
            statistics.record(event(LoanEvent.BORROWED, popular, "Popular Book"));
        }

        assertThat(statistics.mostBorrowed(1))
                .singleElement()
                .satisfies(stat -> assertThat(stat.getBookId()).isEqualTo(popular));
    }

    /** An unrecognised type must not create a phantom entry or blow up the listener. */
    @Test
    void ignoresUnknownEventTypes() {
        UUID bookId = UUID.randomUUID();
        long before = statistics.summary().booksTracked();

        statistics.record(event("BOOK_RESERVED", bookId, "Some Book"));

        assertThat(statistics.summary().booksTracked()).isEqualTo(before);
    }
}
