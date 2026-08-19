package app.adapters.input;

import app.domain.dto.CreateNewAuthor;
import app.domain.dto.CreateNewBook;
import app.domain.dto.CreateNewCustomer;
import app.domain.dto.CreateNewTransaktion;
import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.AuthorRepository;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.TransactionRepository;
import app.adapters.output.repositories.UserRepository;
import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.model.Transaction;
import app.domain.port.input.BookUseCase;
import app.domain.port.input.CustomerUseCase;
import app.domain.port.input.TransactionUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user")
@Tag("integration")
class TransactionControllerTestIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private TransactionUseCase transactionUseCase;
    @Autowired
    private BookUseCase bookUseCase;
    @Autowired
    private CustomerUseCase customerUseCase;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private Customer customer;
    private Book book;
    @BeforeEach
    public void setUp() {
        customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer("Test Customer Transaction", "test_transaction@example.com", true));
        book = bookUseCase.createNewBook(new CreateNewBook("Test Book Transaction", "1234567892",
                2021, List.of(
                new CreateNewAuthor("Test Author Transaction", "test"))));

        // "member" is the account that holds the membership above, so returning and extending can
        // be exercised as the borrower. The class-level "user" deliberately holds none.
        userRepository.save(new UserEntity(
                "member", passwordEncoder.encode("secret"), "USER", customer.getCustomerId()));
    }
    @Test
    void testCreateNewTransaction() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate futureDueDate = today.plusDays(1);

        UUID customerId = customer.getCustomerId();
        UUID bookId = book.getBookId();

        CreateNewTransaktion newTransaktion = new CreateNewTransaktion(
                today,
                futureDueDate,
                customerId,
                bookId
        );
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTransaktion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.borrowDate").value(today.toString()))
                .andExpect(jsonPath("$.dueDate").value(futureDueDate.toString()))
                .andExpect(jsonPath("$.customer.customerId").value(newTransaktion.getCustomerId().toString()))
                .andExpect(jsonPath("$.book.bookId").value(newTransaktion.getBookId().toString()));
    }
    @Test
    void testCreateNewTransaction_BadRequest() throws Exception {
        CreateNewTransaktion invalidTransaction = new CreateNewTransaktion(
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                UUID.randomUUID(),
                null
        );

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransaction)))
                .andExpect(status().isBadRequest());
    }
    @Test
    void testBorrowBook() throws Exception {
        UUID customerId = customer.getCustomerId();
        UUID bookId = book.getBookId();

        mockMvc.perform(post("/transactions/borrowBook/{customerId}/{bookId}", customerId, bookId))
                .andExpect(status().isOk())
                .andExpect(content().string("Book borrowed successfully."));

        long transactionCount = transactionRepository.countByCustomerCustomerId(customerId);
        assertEquals(1, transactionCount);
    }
    @Test
    void testBorrowBook_bookNotAvailable() throws Exception {
        UUID customerId = customer.getCustomerId();
        UUID bookId = book.getBookId();

        transactionUseCase.borrowBook(customerId, bookId);

        mockMvc.perform(post("/transactions/borrowBook/{customerId}/{bookId}", customerId, bookId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to borrow book: Book is not available for borrowing."));
    }
    @Test
    @WithMockUser(username = "member")
    void testReturnBook() throws Exception {
        UUID customerId = customer.getCustomerId();
        UUID bookId = book.getBookId();

        transactionUseCase.borrowBook(customerId, bookId);

        mockMvc.perform(post("/transactions/returnBook/{bookId}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction successful."));
    }

    /**
     * Knowing a book id is not enough to close somebody else's loan. "user" holds no membership,
     * so it is not the borrower and must be refused.
     */
    @Test
    void testReturnBook_refusedForSomeoneElsesLoan() throws Exception {
        transactionUseCase.borrowBook(customer.getCustomerId(), book.getBookId());

        mockMvc.perform(post("/transactions/returnBook/{bookId}", book.getBookId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only return books you have out."));
    }

    /** Staff take returns at the desk, so an administrator may close any loan. */
    @Test
    @WithMockUser(username = "desk", roles = "ADMIN")
    void testReturnBook_allowedForAdministrators() throws Exception {
        transactionUseCase.borrowBook(customer.getCustomerId(), book.getBookId());

        mockMvc.perform(post("/transactions/returnBook/{bookId}", book.getBookId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction successful."));
    }

    @Test
    @WithMockUser(username = "member")
    void testReturnBook_noTransactionFound() throws Exception {
        UUID bookId = book.getBookId();

        mockMvc.perform(post("/transactions/returnBook/{bookId}", bookId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Failed to return book: This book has no open loan to return."));
    }
    @Test
    void testReturnBook_bookNotFound() throws Exception {
        UUID bookId = UUID.randomUUID();

        mockMvc.perform(post("/transactions/returnBook/{bookId}", bookId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found."));
    }
    @Test
    void testGetTransactionID() throws Exception {
        UUID customerId = customer.getCustomerId();
        UUID bookId = book.getBookId();

        Transaction transaction = transactionUseCase.borrowBook(customerId, bookId);

        assertNotNull(transaction);
        assertNotNull(transaction.getTransactionId());

        UUID transactionId = transaction.getTransactionId();

        mockMvc.perform(get("/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.data.borrowDate").exists())
                .andExpect(jsonPath("$.data.dueDate").exists())
                .andExpect(jsonPath("$.data.customer.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.data.book.bookId").value(bookId.toString()));
    }
    @Test
    void testGetTransactionById_NotFound() throws Exception {
        UUID invalidTransactionId = UUID.randomUUID();

        mockMvc.perform(get("/transactions/{id}", invalidTransactionId)
                        .accept("application/single-transaction-response+json;version=1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"))
                .andExpect(jsonPath("$.transactionId").value(invalidTransactionId.toString()));
    }
    @Test
    void testGetTransactionById_InvalidUUID() throws Exception {
        String invalidUuid = "123-invalid-uuid";

        mockMvc.perform(get("/transactions/{id}", invalidUuid)
                        .accept("application/single-transaction-response+json;version=1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format"))
                .andExpect(jsonPath("$.providedId").value(invalidUuid));
    }
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testViewBorrowingHistory() throws Exception {
        UUID customerId = customer.getCustomerId();
        UUID bookId = book.getBookId();

        transactionUseCase.borrowBook(customerId, bookId);

        mockMvc.perform(get("/transactions/history/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].transactionId").exists())
                .andExpect(jsonPath("$.data[0].borrowDate").exists())
                .andExpect(jsonPath("$.data[0].dueDate").exists())
                .andExpect(jsonPath("$.data[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.data[0].bookId").value(bookId.toString()))
                .andExpect(jsonPath("$.data[0].book.bookId").value(bookId.toString()));
    }
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testViewBorrowingHistory_noTransactionsFound() throws Exception {
        UUID customerId = customer.getCustomerId();

        mockMvc.perform(get("/transactions/history/{customerId}", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No transactions found for this customer"));
    }

    /** A member must not be able to read another member's loans by guessing a customer id. */
    @Test
    void testViewBorrowingHistory_refusedForMembers() throws Exception {
        mockMvc.perform(get("/transactions/history/{customerId}", customer.getCustomerId()))
                .andExpect(status().isForbidden());
    }

    /** The seeded "user" account holds no membership, so its own history is simply empty. */
    @Test
    void testMyHistory_withoutMembership() throws Exception {
        mockMvc.perform(get("/transactions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @AfterEach
    public void tearDown() {
        transactionRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}