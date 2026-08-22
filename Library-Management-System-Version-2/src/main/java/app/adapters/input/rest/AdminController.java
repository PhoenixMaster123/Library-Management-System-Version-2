package app.adapters.input.rest;

import app.domain.dto.CreateNewAuthor;
import app.domain.dto.CreateNewBook;
import app.domain.model.Author;
import app.domain.model.Book;
import app.domain.model.CatalogCandidate;
import app.domain.model.LoanStatistics;
import app.domain.model.Transaction;
import app.domain.port.input.AuthorUseCase;
import app.domain.port.input.BookUseCase;
import app.domain.port.input.TransactionUseCase;
import app.domain.port.output.BookCatalogPort;
import app.domain.port.output.LoanStatisticsPort;
import app.domain.services.CatalogImportService;
import app.infrastructure.exceptions.BookNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Catalogue, author and loan administration. Every endpoint here requires the ADMIN role. */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Controller", description = "Catalogue management, administrators only")
@RequiredArgsConstructor
public class AdminController extends PaginatedController {

    /** Bounds on the ranked-book list, so a caller cannot ask Analytics-Service for everything. */
    private static final int MIN_RANKED_BOOKS = 1;
    private static final int MAX_RANKED_BOOKS = 100;

    private final BookUseCase bookUseCase;
    private final AuthorUseCase authorUseCase;
    private final TransactionUseCase transactionUseCase;
    private final BookCatalogPort bookCatalogPort;
    private final CatalogImportService catalogImportService;
    private final LoanStatisticsPort loanStatisticsPort;

    /** Lists who has what out and when it is due. Outstanding loans only unless activeOnly=false. */
    @GetMapping(value = "/loans", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Every loan across all members")
    public ResponseEntity<Map<String, Object>> loans(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy
    ) {
        PageRequest pageable = pageRequest(page, size, sortBy);
        Page<Transaction> loans = activeOnly
                ? transactionUseCase.viewActiveLoans(pageable)
                : transactionUseCase.viewAllLoans(pageable);

        return ResponseEntity.ok(pageBody(loans));
    }

    /** Reads borrowing statistics from Analytics-Service. Answers 503, never zeros, when it is down. */
    @GetMapping(value = "/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Borrowing statistics across the library")
    public ResponseEntity<Map<String, Object>> analytics(
            @RequestParam(defaultValue = "10") int limit
    ) {
        int ranked = Math.clamp(limit, MIN_RANKED_BOOKS, MAX_RANKED_BOOKS);

        return loanStatisticsPort.fetch(ranked)
                .map(AdminController::analyticsBody)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("message", "Analytics is unavailable.")));
    }

    /** Shapes the statistics into the response body: totals under "summary", then the ranked books. */
    private static Map<String, Object> analyticsBody(LoanStatistics statistics) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("booksTracked", statistics.booksTracked());
        summary.put("totalBorrows", statistics.totalBorrows());
        summary.put("totalReturns", statistics.totalReturns());
        summary.put("currentlyOut", statistics.currentlyOut());
        // Without these the figures cannot be read honestly: see LoanStatistics.
        summary.put("streamConnected", statistics.streamConnected());
        summary.put("lastEventAt", statistics.lastEventAt());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", summary);
        body.put("popularBooks", statistics.popularBooks());
        return body;
    }

    /** Prefills the add-book form from one ISBN. Answers 404 when the catalogue has no match. */
    @GetMapping(value = "/books/lookup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Look a book up by ISBN in the external catalogue")
    public ResponseEntity<CreateNewBook> lookupBook(@RequestParam String isbn) {
        return ResponseEntity.ok(bookCatalogPort.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException("No book found for ISBN " + isbn)));
    }

    /** Searches the external catalogue for candidates to stock; at most `limit` of them. */
    @GetMapping(value = "/books/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search the external catalogue for books to stock")
    public ResponseEntity<List<CatalogCandidate>> searchCatalog(@RequestParam String query,
                                                               @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(bookCatalogPort.search(query, limit));
    }

    /** Stocks many books at once from their ISBNs. Reports what was imported and what was skipped. */
    @PostMapping(value = "/books/import", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Import books from the external catalogue by ISBN")
    public ResponseEntity<Map<String, Object>> importBooks(@RequestBody ImportBooksRequest request) {
        CatalogImportService.ImportSummary summary = catalogImportService.importAll(request.isbns());

        return ResponseEntity.ok(Map.of(
                "message", "Imported " + summary.imported().size()
                        + " book(s), skipped " + summary.skipped().size() + ".",
                "imported", summary.imported(),
                "skipped", summary.skipped()));
    }

    /** Body of the bulk import: the ISBNs the librarian ticked in the search results. */
    public record ImportBooksRequest(List<String> isbns) {
    }

    /** Adds one book and returns it with the id the catalogue assigned. */
    @PostMapping(value = "/books", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a book to the catalogue")
    public ResponseEntity<Book> createBook(@Valid @RequestBody CreateNewBook newBook) {
        return ResponseEntity.ok(bookUseCase.createNewBook(newBook));
    }

    /** Replaces a book's details. Answers 404 when no book carries that id. */
    @PutMapping(value = "/books/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a book")
    public ResponseEntity<Map<String, String>> updateBook(@PathVariable UUID id,
                                                          @Valid @RequestBody Book book) {
        if (bookUseCase.searchById(id).isEmpty()) {
            throw new BookNotFoundException("Book not found");
        }
        book.setBookId(id);
        bookUseCase.updateBook(id, book);
        return ResponseEntity.ok(Map.of("message", "Book updated successfully"));
    }

    /** Removes a book from the catalogue. */
    @DeleteMapping(value = "/books/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Remove a book from the catalogue")
    public ResponseEntity<Map<String, String>> deleteBook(@PathVariable UUID id) {
        bookUseCase.deleteBook(id);
        return ResponseEntity.ok(Map.of("message", "Book successfully deleted!"));
    }

    /** Adds one author and returns it with the id the catalogue assigned. */
    @PostMapping(value = "/authors", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add an author")
    public ResponseEntity<Author> createAuthor(@Valid @RequestBody CreateNewAuthor newAuthor) {
        return ResponseEntity.ok(authorUseCase.createNewAuthor(newAuthor));
    }

    /** Replaces an author's details and answers 202. */
    @PutMapping(value = "/authors/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an author")
    public ResponseEntity<Map<String, String>> updateAuthor(@PathVariable UUID id,
                                                            @Valid @RequestBody Author author) {
        author.setAuthorId(id);
        authorUseCase.updateAuthor(id, author);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("message", "Author updated successfully!"));
    }
}
