package app.adapters.output.analytics;

import java.util.UUID;

/** One entry of Analytics-Service's popular-books reply. Adapter-local: the domain must not know it. */
public record PopularBookResponse(
        UUID bookId,
        String title,
        String isbn,
        long timesBorrowed,
        long timesReturned,
        long currentlyOut) {
}
