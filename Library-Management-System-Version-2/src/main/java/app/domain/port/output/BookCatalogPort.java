package app.domain.port.output;

import app.domain.dto.CreateNewBook;
import app.domain.model.CatalogCandidate;
import app.domain.model.CatalogPage;

import java.util.List;
import java.util.Optional;

/** An external catalogue to prefill book details from. Never throws: a lookup is a convenience. */
public interface BookCatalogPort {

    /** The catalogue's entry for one ISBN, or empty when unknown or unreachable. */
    Optional<CreateNewBook> findByIsbn(String isbn);

    /** One page of free-text search. Candidates carry no description; that is fetched on import. */
    CatalogPage search(String query, int page, int size);

    /** The first page only, for callers that just want a handful of candidates. */
    default List<CatalogCandidate> search(String query, int limit) {
        return search(query, 0, limit).results();
    }
}
