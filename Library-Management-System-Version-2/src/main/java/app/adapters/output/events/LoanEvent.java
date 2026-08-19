package app.adapters.output.events;

import java.time.Instant;
import java.util.UUID;

/**
 * What travels on the {@code library.loans} topic. Deliberately flat and self-contained: the
 * analytics service must be able to read it without calling back into the library.
 */
public record LoanEvent(
        String type,
        UUID customerId,
        String customerName,
        UUID bookId,
        String bookTitle,
        String bookIsbn,
        Instant occurredAt) {

    public static final String BORROWED = "BOOK_BORROWED";
    public static final String RETURNED = "BOOK_RETURNED";
}
