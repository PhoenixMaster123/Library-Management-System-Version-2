package app.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Borrowing statistics from Analytics-Service. streamConnected false means unreachable, not idle. */
public record LoanStatistics(
        long booksTracked,
        long totalBorrows,
        long totalReturns,
        long currentlyOut,
        boolean streamConnected,
        Instant lastEventAt,
        List<BookStat> popularBooks) {

    /** Copies the ranked list defensively and turns a null one into an empty one. */
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
