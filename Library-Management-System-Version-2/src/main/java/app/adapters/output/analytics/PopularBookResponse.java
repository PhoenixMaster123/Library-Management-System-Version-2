package app.adapters.output.analytics;

import java.util.UUID;

/**
 * One entry of Analytics-Service's {@code GET /api/v1/analytics/popular-books}.
 * Adapter-local on purpose: the domain must not know this shape.
 */
public record PopularBookResponse(
        UUID bookId,
        String title,
        String isbn,
        long timesBorrowed,
        long timesReturned,
        long currentlyOut) {
}
