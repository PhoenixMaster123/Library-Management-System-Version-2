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

/** Puts books from the external catalogue onto the shelves. Already-stocked ones are skipped. */
@Service
@RequiredArgsConstructor
public class CatalogImportService {

    /** Stands in when the catalogue names no author at all. */
    private static final String UNKNOWN_AUTHOR = "Unknown author";

    private final BookCatalogPort bookCatalogPort;
    private final BookUseCase bookUseCase;

    /** What a bulk import did, per ISBN, so the caller can report it without guessing. */
    public record ImportSummary(List<String> imported, List<String> skipped) {
    }

    /** Stocks books from ISBNs, looking each one up. One bad ISBN never fails the rest of the batch. */
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

    /** Stocks search results in bulk with no per-book lookup, which is what makes the seed affordable. */
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

    /** Stocks the exact edition picked, not a re-lookup, which could return another language. */
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
