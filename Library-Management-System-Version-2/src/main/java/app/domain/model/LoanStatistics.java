package app.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Borrowing statistics, as read back from Analytics-Service.
 *
 * <p>{@code streamConnected} is false when no event can reach it, which is not the same as no
 * borrowing having happened.
 */
public record LoanStatistics(
        long booksTracked,
        long totalBorrows,
        long totalReturns,
        long currentlyOut,
        boolean streamConnected,
        Instant lastEventAt,
        List<BookStat> popularBooks) {

    public LoanStatistics {
        popularBooks = popularBooks == null ? List.of() : List.copyOf(popularBooks);
    }

    /** One book's share of the totals. */
    public record BookStat(
            UUID bookId,
            String title,
            String isbn,
            long timesBorrowed,
            long timesReturned,
            long currentlyOut) {
    }
}
