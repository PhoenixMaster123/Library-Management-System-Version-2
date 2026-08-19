package app.domain.services;

import app.domain.dto.CreateNewAuthor;
import app.domain.dto.CreateNewBook;
import app.domain.model.Book;
import app.domain.model.CatalogCandidate;
import app.domain.port.input.BookUseCase;
import app.domain.port.output.BookCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Puts books from the external catalogue onto the shelves.
 *
 * <p>{@link #importAll} looks ISBNs up; {@link #addCandidate} stocks a search result as it stands.
 * Already-stocked books are skipped rather than treated as failures.
 */
@Service
@RequiredArgsConstructor
public class CatalogImportService {

    private static final String UNKNOWN_AUTHOR = "Unknown author";

    private final BookCatalogPort bookCatalogPort;
    private final BookUseCase bookUseCase;

    /** What a bulk import did, per ISBN, so the caller can report it without guessing. */
    public record ImportSummary(List<String> imported, List<String> skipped) {
    }

    /**
     * Stocks books by ISBN alone, looking each one up for its blurb and authors. Used by the desk's
     * bulk import, where the librarian has a list of ISBNs rather than search results in hand.
     *
     * <p>Neither a missing book nor an already-stocked one fails the rest of the batch.
     */
    public ImportSummary importAll(List<String> isbns) {
        List<String> imported = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (String isbn : isbns == null ? List.<String>of() : isbns) {
            Optional<CreateNewBook> candidate = bookCatalogPort.findByIsbn(isbn);
            if (candidate.isEmpty() || candidate.get().getTitle() == null) {
                skipped.add(isbn + " (not found)");
                continue;
            }

            CreateNewBook book = withAuthors(candidate.get());
            try {
                bookUseCase.createNewBook(book);
                imported.add(book.getTitle());
            } catch (IllegalArgumentException alreadyStocked) {
                skipped.add(book.getTitle() + " (" + alreadyStocked.getMessage() + ")");
            }
        }

        return new ImportSummary(imported, skipped);
    }

    /**
     * Stocks search results in bulk, with no lookup per book.
     *
     * <p>That is what makes the startup seed affordable: four hundred books cost twelve searches
     * here where {@link #importAll} would cost four hundred round trips. The blurbs they lack are
     * fetched by {@code CatalogEnrichmentService} as books are opened.
     */
    public ImportSummary importCandidates(Collection<CatalogCandidate> candidates) {
        List<String> imported = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (CatalogCandidate candidate : candidates == null ? List.<CatalogCandidate>of() : candidates) {
            if (candidate.title() == null || candidate.isbn() == null) {
                skipped.add(candidate.isbn() + " (incomplete)");
                continue;
            }
            try {
                imported.add(addCandidate(candidate).getTitle());
            } catch (IllegalArgumentException alreadyStocked) {
                skipped.add(candidate.title() + " (" + alreadyStocked.getMessage() + ")");
            }
        }

        return new ImportSummary(imported, skipped);
    }

    /**
     * Stocks exactly the book the reader picked out of the search results.
     *
     * <p>Deliberately built from the candidate rather than from a fresh ISBN lookup. A search hit's
     * ISBN belongs to one particular edition, so looking it up again can come back in another
     * language - clicking "A Wizard of Earthsea" put "Czarnoksiężnik z Archipelagu" on the shelf.
     * The blurb is filled in later by {@code CatalogEnrichmentService}, on first view.
     *
     * @throws IllegalArgumentException when the book is already on the shelves
     */
    public Book addCandidate(CatalogCandidate candidate) {
        List<CreateNewAuthor> authors = candidate.authors() == null
                ? List.of()
                : candidate.authors().stream().map(name -> new CreateNewAuthor(name, "")).toList();

        return bookUseCase.createNewBook(withAuthors(new CreateNewBook(
                candidate.title(),
                candidate.isbn(),
                candidate.publicationYear(),
                null,
                authors)));
    }

    /** A book with no author at all would fail validation, and "unknown" is the honest answer. */
    private CreateNewBook withAuthors(CreateNewBook book) {
        if (book.getAuthors() == null || book.getAuthors().isEmpty()) {
            book.setAuthors(List.of(new CreateNewAuthor(UNKNOWN_AUTHOR, "")));
        }
        return book;
    }
}
