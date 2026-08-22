package app.adapters.input.rest;

import app.domain.dto.CreateNewTransaktion;
import app.domain.dto.TransactionResponse;
import app.domain.model.Transaction;
import app.domain.port.input.BookUseCase;
import app.domain.port.input.TransactionUseCase;
import app.infrastructure.config.security.CurrentAccount;
import app.infrastructure.exceptions.BorrowNotAllowedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/** Borrowing, returning and extending loans. */
@RestController
@RequestMapping("/transactions")
@Tag(name = "Transaction Controller", description = "Endpoints for managing transactions")
@RequiredArgsConstructor
public class TransactionController extends PaginatedController {
    private final TransactionUseCase transactionUseCase;
    private final BookUseCase bookUseCase;
    private final CurrentAccount currentAccount;

    @PostMapping(produces = {"application/single-transaction-response+json;version=1",
            MediaType.APPLICATION_JSON_VALUE})

    /** Records a loan from explicit dates. Administrators only. */
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<Transaction> createNewTransaction(
            @Valid @RequestBody CreateNewTransaktion newTransaktion, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(null);
        }

        Transaction transaction = transactionUseCase.createNewTransaction(newTransaktion);
        return ResponseEntity.ok(transaction);
    }

    /** Closes a loan. Only the member holding the book, or an administrator, may return it. */
    @PostMapping(value = "/returnBook/{bookId}",
            produces = {"application/transaction-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Return a book")
    public ResponseEntity<TransactionResponse> returnBook(@PathVariable UUID bookId,
                                                          Authentication authentication) {
        if (bookUseCase.searchById(bookId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new TransactionResponse("Book not found.", null));
        }

        Optional<Transaction> activeLoan = transactionUseCase.findActiveLoanForBook(bookId);
        if (activeLoan.isEmpty()) {
            return ResponseEntity.badRequest().body(new TransactionResponse(
                    "Failed to return book: This book has no open loan to return.", null));
        }
        if (!isAdmin(authentication) && !isOwner(authentication, activeLoan.get().getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new TransactionResponse("You can only return books you have out.", null));
        }

        try {
            String transactionId = transactionUseCase.returnBook(bookId);
            return ResponseEntity.ok(
                    new TransactionResponse("Transaction successful.", UUID.fromString(transactionId)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(new TransactionResponse("Failed to return book: " + e.getMessage(), null));
        }
    }

    /** Only the member themselves or an administrator may borrow against a membership. */
    @PostMapping(value = "/borrowBook/{customerId}/{bookId}",
            produces = {"application/transaction-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Borrow a book")
    public ResponseEntity<String> borrowBook(
            @PathVariable UUID customerId,
            @PathVariable UUID bookId,
            Authentication authentication) {
        // The customer id comes from the path, so without this a member could borrow against
        // somebody else's membership and spend their loan limit. Matches returnBook and extendLoan.
        if (!isAdmin(authentication) && !isOwner(authentication, customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only borrow against your own membership.");
        }

        try {
            transactionUseCase.borrowBook(customerId, bookId);
            return ResponseEntity.ok("Book borrowed successfully.");
        } catch (BorrowNotAllowedException e) {
            // A library rule said no; a missing book or customer falls through to the 404 handler.
            return ResponseEntity.badRequest().body("Failed to borrow book: " + e.getMessage());
        }
    }

    /** Grants one further loan period. Only the borrower or an administrator may extend. */
    @PostMapping(value = "/{transactionId}/extend", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Extend a loan by one further period")
    public ResponseEntity<Map<String, Object>> extendLoan(@PathVariable UUID transactionId,
                                                          Authentication authentication) {
        Transaction loan = transactionUseCase.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found."));

        if (!isAdmin(authentication) && !isOwner(authentication, loan.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only extend your own loans."));
        }

        Transaction extended = transactionUseCase.extendLoan(transactionId);
        return ResponseEntity.ok(Map.of(
                "message", "Loan extended until " + extended.getDueDate(),
                "dueDate", extended.getDueDate().toString()));
    }

    /** The caller's own loans. Takes no customer id, so nobody can ask for another member's. */
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "View my borrowing history")
    public ResponseEntity<Map<String, Object>> myHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "borrowDate") String sortBy
    ) {
        Optional<UUID> customerId = currentAccount.customerId(authentication);
        if (customerId.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "This account has no library membership.",
                    "data", List.of(),
                    "totalPages", 0,
                    "currentPage", 0,
                    "totalItems", 0));
        }

        Page<Transaction> loans = transactionUseCase.viewBorrowingHistory(
                customerId.get(), pageRequest(page, size, sortBy));

        return ResponseEntity.ok(pageBody(loans));
    }

    /** True when the signed-in account holds the ADMIN role. */
    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    /** True when the signed-in account's membership is the one the loan belongs to. */
    private boolean isOwner(Authentication authentication, UUID loanCustomerId) {
        return currentAccount.customerId(authentication)
                .filter(id -> id.equals(loanCustomerId))
                .isPresent();
    }

    @GetMapping(value = "/history/{customerId}",
            produces = {"application/paginated-transactions-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})

    /** One page of one member's loans. Administrators only. */
    @Operation(summary = "View borrowing history for a customer (administrators)")
    public ResponseEntity<Map<String, Object>> viewBorrowingHistory(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "borrowDate") String sortBy
    ) {
        PageRequest pageable = pageRequest(page, size, sortBy);
        Page<Transaction> transactionsPage = transactionUseCase.viewBorrowingHistory(customerId, pageable);

        HttpHeaders headers = pageLinks(transactionsPage, p -> methodOn(TransactionController.class)
                .viewBorrowingHistory(customerId, p, size, sortBy));

        // Alone among the paginated endpoints, this one answers the empty case without the headers.
        if (transactionsPage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No transactions found for this customer"));
        }

        return ResponseEntity.ok().headers(headers).body(pageBody(transactionsPage));
    }

    @GetMapping(value = "/{id}",
            produces = {"application/single-transaction-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})

    /** One loan by id; 404 when it matches nothing. */
    @Operation(summary = "Get a single transaction by ID")
    public ResponseEntity<Map<String, Object>> getTransactionById(@PathVariable UUID id) {
        Optional<Transaction> transactionOpt = transactionUseCase.findById(id);

        if (transactionOpt.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "Transaction not found",
                    "transactionId", id
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        CacheControl cacheControl = CacheControl
                .maxAge(30, TimeUnit.SECONDS)
                .cachePrivate()
                .noTransform();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("Vary", "Accept")
                .body(Map.of("message", "Transaction retrieved successfully", "data", transactionOpt.get()));
    }
}
