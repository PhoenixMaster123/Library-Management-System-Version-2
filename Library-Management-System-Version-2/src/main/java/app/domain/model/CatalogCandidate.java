package app.domain.model;

import java.util.List;

/**
 * A book found in the external catalogue but not yet stocked.
 *
 * <p>Separate from {@code CreateNewBook} because it carries what the picker needs rather than
 * what the create endpoint accepts - notably {@code coverId}, which addresses the cover image
 * directly instead of making the cover server resolve an ISBN first.
 */
public record CatalogCandidate(
        String title,
        String isbn,
        int publicationYear,
        List<String> authors,
        Long coverId) {
}
