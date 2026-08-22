package app.domain.model;

import java.util.List;

/** A book found in the external catalogue but not yet stocked. Carries coverId for the picker. */
public record CatalogCandidate(
        String title,
        String isbn,
        int publicationYear,
        List<String> authors,
        Long coverId) {
}
