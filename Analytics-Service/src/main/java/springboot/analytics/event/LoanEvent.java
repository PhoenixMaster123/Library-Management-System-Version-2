package springboot.analytics.event;

import java.time.Instant;
import java.util.UUID;

/**
 * What the library publishes on {@code library.loans}. Kept structurally identical to the
 * producer's record - the topic is the contract between the two services.
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
