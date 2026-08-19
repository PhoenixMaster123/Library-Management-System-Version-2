package app.domain.port.output;

import app.domain.dto.CreateNewBook;
import app.domain.model.CatalogCandidate;
import app.domain.model.CatalogPage;

import java.util.List;
import java.util.Optional;

/**
 * Looks a book up in an external catalogue by ISBN so a librarian does not have to type the
 * title, authors, year and blurb by hand.
 *
 * <p>Returns empty rather than throwing when the book is unknown or the catalogue is
 * unreachable: a lookup is a convenience, never a precondition for adding a book.
 */
public interface BookCatalogPort {

    Optional<CreateNewBook> findByIsbn(String isbn);

    /**
     * One page of free-text search over the external catalogue. Candidates carry no description;
     * that is fetched per book on import.
     */
    CatalogPage search(String query, int page, int size);

    /** The first page only, for callers that just want a handful of candidates. */
    default List<CatalogCandidate> search(String query, int limit) {
        return search(query, 0, limit).results();
    }
}
