package app.adapters.input.rest;

import app.domain.model.Book;
import app.domain.model.CatalogCandidate;
import app.domain.model.CatalogPage;
import app.domain.model.Transaction;
import app.domain.port.input.BookUseCase;
import app.domain.port.input.TransactionUseCase;
import app.domain.port.output.BookCatalogPort;
import app.domain.services.CatalogEnrichmentService;
import app.domain.services.CatalogImportService;
import app.infrastructure.config.security.CurrentAccount;
import app.infrastructure.exceptions.BookNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/** Read-only catalogue browsing. Creating, editing and deleting books lives in {@link AdminController}. */
@RestController
@RequestMapping("/books")
@Tag(name = "Book Controller", description = "Browsing and searching the catalogue")
@RequiredArgsConstructor
public class BookController extends PaginatedController {

    private final BookUseCase bookUseCase;
    private final TransactionUseCase transactionUseCase;
    private final BookCatalogPort bookCatalogPort;
    private final CatalogImportService catalogImportService;
    private final CatalogEnrichmentService catalogEnrichmentService;
    private final CurrentAccount currentAccount;

    /**
     * One page of the shelves. Passing {@code query} narrows it to matching books rather than
     * every book, so browsing and searching share this one paged shape instead of the caller
     * having to switch endpoints - and page numbers - halfway through.
     */
    @GetMapping(value = "/paginated",
            produces = {"application/paginated-books-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Get all books, or those matching a query")
    public ResponseEntity<Map<String, Object>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(required = false) String query
    ) {
        PageRequest pageable = pageRequest(page, size, sortBy);
        String trimmed = query == null ? "" : query.trim();

        Page<Book> books = trimmed.isEmpty()
                ? bookUseCase.getPaginatedBooks(pageable)
                : bookUseCase.searchBooks(trimmed, pageable);

        HttpHeaders headers = pageLinks(books, p -> methodOn(BookController.class).getAllBooks(p, size, sortBy, query));

        if (books.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", trimmed.isEmpty()
                            ? "There are no books on this page."
                            : "No books match that search.",
                    "currentPage", page,
                    "pageSize", size,
                    "sortBy", sortBy
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(errorResponse);
        }

        return ResponseEntity.ok().headers(headers).body(pageBody(books));
    }

    /**
     * The full record behind a row. {@code borrowedByMe} is what lets the reader be offered
     * Return rather than Borrow - the availability flag alone cannot tell "you have this out"
     * from "somebody else does".
     */
    @GetMapping(value = "/{id}",
            produces = {"application/single-book-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Get a book by id")
    public ResponseEntity<Map<String, Object>> getBookById(@PathVariable UUID id,
                                                           Authentication authentication) {
        Optional<Book> found = bookUseCase.searchById(id);

        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Book not found", "bookId", id));
        }

        Book book = catalogEnrichmentService.withDescription(found.get());
        Optional<Transaction> activeLoan = transactionUseCase.findActiveLoanForBook(id);
        Optional<UUID> me = currentAccount.customerId(authentication);

        boolean borrowedByMe = activeLoan.isPresent()
                && me.isPresent()
                && me.get().equals(activeLoan.get().getCustomerId());

        CacheControl cacheControl = CacheControl
                .maxAge(30, TimeUnit.SECONDS)
                .cachePrivate()
                .noTransform();

        // HashMap rather than Map.of: dueDate is absent for a book on the shelf, and Map.of
        // rejects a null value.
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Book retrieved successfully");
        body.put("data", book);
        body.put("borrowedByMe", borrowedByMe);
        body.put("dueDate", activeLoan.map(loan -> loan.getDueDate().toString()).orElse(null));

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("Vary", "Accept")
                .body(body);
    }

    /**
     * Searches the external catalogue rather than the shelves, so a member can find a book the
     * library does not hold yet. Each hit says whether it is already stocked, which is what turns
     * "Add to library" into "Already on the shelves" without a second round trip.
     */
    @GetMapping(value = "/discover", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search the external catalogue for books the library could stock")
    public ResponseEntity<Map<String, Object>> discover(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CatalogPage found = bookCatalogPort.search(query, page, size);

        List<Map<String, Object>> results = found.results().stream()
                .map(this::describeCandidate)
                .toList();

        return ResponseEntity.ok(Map.of(
                "data", results,
                "currentPage", page,
                "totalItems", found.totalItems(),
                "totalPages", size <= 0 ? 0 : (found.totalItems() + size - 1) / size));
    }

    /**
     * Any member may stock a book they want to read; the catalogue is the library's, not the desk's.
     *
     * <p>The whole search hit is sent back, not just its ISBN, so the shelf gets the book the
     * reader actually saw. Re-deriving it from the ISBN looks the edition up afresh and can return
     * a different language - clicking "A Wizard of Earthsea" once stocked its Polish edition.
     */
    @PostMapping(value = "/discover", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a book from the external catalogue to the library")
    public ResponseEntity<Map<String, Object>> addFromCatalog(@Valid @RequestBody AddFromCatalogRequest request) {
        try {
            Book added = catalogImportService.addCandidate(new CatalogCandidate(
                    request.title(),
                    request.isbn(),
                    request.publicationYear(),
                    request.authors() == null ? List.of() : request.authors(),
                    null));

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "\"" + added.getTitle() + "\" is now on the shelves.",
                    "bookId", added.getBookId()));
        } catch (IllegalArgumentException alreadyStocked) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "This book is already on the shelves."));
        }
    }

    /** A search hit sent back to be stocked. */
    public record AddFromCatalogRequest(
            @NotBlank(message = "Title is required") String title,
            @NotBlank(message = "ISBN is required") String isbn,
            int publicationYear,
            List<String> authors) {
    }

    private Map<String, Object> describeCandidate(CatalogCandidate candidate) {
        Map<String, Object> described = new HashMap<>();
        described.put("title", candidate.title());
        described.put("isbn", candidate.isbn());
        described.put("publicationYear", candidate.publicationYear());
        described.put("authors", candidate.authors());
        described.put("coverId", candidate.coverId());
        described.put("stocked", bookUseCase.searchByIsbn(candidate.isbn()).isPresent());
        return described;
    }

    /**
     * One search endpoint over several criteria. Every branch answers with a list of books - even
     * the single-book ones - so the response has one shape instead of five, and misses are left to
     * GlobalExceptionHandler rather than each returning its own bare string.
     */
    @GetMapping(produces = {"application/single-book-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Search books by id, title, ISBN, author or free text")
    public ResponseEntity<List<Book>> getBook(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "title") String sortBy
    ) {
        if (id != null) {
            return ResponseEntity.ok(List.of(bookUseCase.searchById(id)
                    .orElseThrow(() -> new BookNotFoundException("Book not found"))));
        }
        if (title != null) {
            return ResponseEntity.ok(List.of(bookUseCase.searchBookByTitle(title)
                    .orElseThrow(() -> new BookNotFoundException("Book with the given title not found"))));
        }
        if (isbn != null) {
            return ResponseEntity.ok(List.of(bookUseCase.searchByIsbn(isbn)
                    .orElseThrow(() -> new BookNotFoundException("Book with the given ISBN not found"))));
        }
        if (author != null) {
            return ResponseEntity.ok(List.of(bookUseCase.searchBookByAuthors(author, true)
                    .orElseThrow(() -> new BookNotFoundException("No books found by the given author"))));
        }
        if (query != null) {
            PageRequest pageable = pageRequest(page, size, sortBy);
            Page<Book> books = bookUseCase.searchBooks(query, pageable);

            HttpHeaders headers = pageLinks(books, p -> methodOn(BookController.class)
                    .getBook(null, null, null, null, query, p, size, sortBy));

            if (books.isEmpty()) {
                throw new BookNotFoundException("No books found for the given query");
            }
            return ResponseEntity.ok().headers(headers).body(books.getContent());
        }

        throw new IllegalArgumentException("No search criteria provided");
    }
}
