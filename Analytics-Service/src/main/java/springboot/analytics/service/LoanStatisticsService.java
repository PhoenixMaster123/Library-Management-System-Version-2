package springboot.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import springboot.analytics.event.LoanEvent;
import springboot.analytics.model.BookStat;
import springboot.analytics.health.StreamHealth;
import springboot.analytics.repository.BookStatRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanStatisticsService {

    private final BookStatRepository repository;
    private final StreamHealth streamHealth;

    /**
     * Folds one event into the running totals. Unknown event types are ignored rather than
     * rejected, so the library can add new ones without this service having to ship first.
     */
    @Transactional
    public void record(LoanEvent event) {
        if (event == null || event.bookId() == null) {
            return;
        }

        BookStat stat = repository.findById(event.bookId())
                .orElseGet(() -> new BookStat(event.bookId(), event.bookTitle(), event.bookIsbn()));

        switch (event.type()) {
            case LoanEvent.BORROWED -> stat.setTimesBorrowed(stat.getTimesBorrowed() + 1);
            case LoanEvent.RETURNED -> stat.setTimesReturned(stat.getTimesReturned() + 1);
            default -> {
                log.debug("Ignoring unknown loan event type {}", event.type());
                return;
            }
        }

        // Titles get corrected in the library from time to time; keep the latest one seen.
        if (event.bookTitle() != null) {
            stat.setTitle(event.bookTitle());
        }
        stat.setLastActivity(event.occurredAt());
        repository.save(stat);
    }

    @Transactional(readOnly = true)
    public List<BookStat> mostBorrowed(int limit) {
        return repository.findMostBorrowed().stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        long borrows = repository.totalBorrows();
        long returns = repository.totalReturns();
        return new Summary(
                repository.count(),
                borrows,
                returns,
                Math.max(0, borrows - returns),
                streamHealth.connected(),
                streamHealth.lastEventAt());
    }

    /**
     * The totals, plus enough about the event stream to interpret them.
     *
     * <p>{@code streamConnected} is what stops a caller reading an empty projection as "nothing has
     * ever been borrowed". With no broker reachable the totals are not a small number - they are
     * no number at all, and the caller has to be able to tell the difference.
     */
    public record Summary(
            long booksTracked,
            long totalBorrows,
            long totalReturns,
            long currentlyOut,
            boolean streamConnected,
            Instant lastEventAt) {
    }
}
