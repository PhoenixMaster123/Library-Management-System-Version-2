package app.adapters.output.events;

import java.time.Instant;
import java.util.UUID;

/** What travels on the library.loans topic. Flat and self-contained, so analytics needs no callback. */
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
