package app.adapters.input;

import app.domain.dto.CreateNewAuthor;
import app.domain.dto.CreateNewCustomer;
import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.AuthorRepository;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.TransactionRepository;
import app.adapters.output.repositories.UserRepository;
import app.domain.model.Author;
import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.port.input.BookUseCase;
import app.domain.port.input.CustomerUseCase;
import app.domain.port.input.TransactionUseCase;
import app.domain.dto.CreateNewBook;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user")
@Tag("integration")
public class BookControllerTestIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookUseCase bookUseCase;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CustomerUseCase customerUseCase;
    @Autowired
    private TransactionUseCase transactionUseCase;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testGetAllBooks() throws Exception {
        mockMvc.perform(get("/books/paginated")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(header().string("self", Matchers.containsString("/books/paginated?page=0&size=5")))
                .andExpect(header().string("next", Matchers.containsString("/books/paginated?page=1&size=5")))
                .andExpect(header().doesNotExist("prev"))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].title").value("1984"))
                .andExpect(jsonPath("$.data[1].title").value("Moby Dick"))
                .andExpect(jsonPath("$.data[2].title").value("Pride and Prejudice"))
                .andExpect(jsonPath("$.data[3].title").value("The Catcher in the Rye"))
                .andExpect(jsonPath("$.data[4].title").value("The Divine Comedy"));

        mockMvc.perform(get("/books/paginated")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(header().string("self", Matchers.containsString("/books/paginated?page=1&size=5")))
                .andExpect(header().doesNotExist("next"))
                .andExpect(header().string("prev", Matchers.containsString("/books/paginated?page=0&size=5")))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].title").value("The Great Gatsby"))
                .andExpect(jsonPath("$.data[1].title").value("The Hobbit"))
                .andExpect(jsonPath("$.data[2].title").value("The Odyssey"))
                .andExpect(jsonPath("$.data[3].title").value("To Kill a Mockingbird"))
                .andExpect(jsonPath("$.data[4].title").value("War and Peace"));
    }
    @Test
    public void testGetBookById() throws Exception {
        Book book = bookUseCase.createNewBook(
                new CreateNewBook("Test Book", "1234567890",
                        2021, List.of(
                        new CreateNewAuthor("Test Author", "test"))));

        mockMvc.perform(get("/books")
                        .param("id", book.getBookId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].isbn").value("1234567890"))
                .andExpect(jsonPath("$[0].publicationYear").value(2021))
                .andExpect(jsonPath("$[0].authors[0].name").value("Test Author"));

    }
    @Test
    public void testGetBookById_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("id", "12345678-1234-1234-1234-123456789012"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found"));
    }
    @Test
    void testGetBookById_Method() throws Exception {
        Book book = bookUseCase.createNewBook(
                new CreateNewBook("Test Book", "1234567890",
                        2021, List.of(
                        new CreateNewAuthor("Test Author", "test"))));

        mockMvc.perform(get("/books/{id}", book.getBookId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookId").value(book.getBookId().toString()));
    }

    @Test
    void testGetBookById_NotFound_Method() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/books/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found"));
    }
    @Test
    public void testGetBookByTitle() throws Exception {
        bookUseCase.createNewBook(
                new CreateNewBook("Test Book", "1234567890",
                        2021, List.of(
                        new CreateNewAuthor("Test Author", "test"))));

        mockMvc.perform(get("/books")
                        .param("title", "Test Book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].isbn").value("1234567890"))
                .andExpect(jsonPath("$[0].publicationYear").value(2021))
                .andExpect(jsonPath("$[0].authors[0].name").value("Test Author"));
    }
    @Test
    public void testGetBookByTitle_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("title", "Test Book"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book with the given title not found"));
    }
    @Test
    public void testGetBookByIsbn() throws Exception {
        bookUseCase.createNewBook(
                new CreateNewBook("Test Book", "1234567890",
                        2021, List.of(
                        new CreateNewAuthor("Test Author", "test"))));

        mockMvc.perform(get("/books")
                        .param("isbn", "1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].isbn").value("1234567890"))
                .andExpect(jsonPath("$[0].publicationYear").value(2021))
                .andExpect(jsonPath("$[0].authors[0].name").value("Test Author"));
    }
    @Test
    public void testGetBookByIsbn_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("isbn", "1234567890"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book with the given ISBN not found"));
    }
    @Test
    public void testGetBookByAuthor() throws Exception {
        bookUseCase.createNewBook(
                new CreateNewBook("Test Book", "1234567890",
                        2021, List.of(
                                new CreateNewAuthor("Test Author", "test"))));

        mockMvc.perform(get("/books")
                        .param("author", "Test Author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].isbn").value("1234567890"))
                .andExpect(jsonPath("$[0].publicationYear").value(2021))
                .andExpect(jsonPath("$[0].authors[0].name").value("Test Author"));
    }
    @Test
    public void testGetBookByAuthor_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("author", "Test Author"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No books found by the given author"));
    }
    @Test
    public void testGetBookByQuery() throws Exception {
        bookUseCase.createNewBook(
                new CreateNewBook("Test Book", "1234567890",
                        2021, List.of(
                        new CreateNewAuthor("Test Author", "test"))));

        mockMvc.perform(get("/books")
                        .param("query", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].isbn").value("1234567890"))
                .andExpect(jsonPath("$[0].publicationYear").value(2021))
                .andExpect(jsonPath("$[0].authors[0].name").value("Test Author"));
    }
    @Test
    public void testGetBookByQuery_multipleResults() throws Exception {
        mockMvc.perform(get("/books")
                        .param("query", "The")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/books?query=The&page=0&size=2")))
                .andExpect(header().string("next", Matchers.containsString("/books?query=The&page=1&size=2")))
                .andExpect(header().doesNotExist("prev"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("The Catcher in the Rye"))
                .andExpect(jsonPath("$[1].title").value("The Divine Comedy"));

        mockMvc.perform(get("/books")
                        .param("query", "The")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/books?query=The&page=1&size=2")))
                .andExpect(header().string("next", Matchers.containsString("/books?query=The&page=2&size=2")))
                .andExpect(header().string("prev", Matchers.containsString("/books?query=The&page=0&size=2")))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("The Great Gatsby"))
                .andExpect(jsonPath("$[1].title").value("The Hobbit"));

        mockMvc.perform(get("/books")
                        .param("query", "The")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/books?query=The&page=2&size=2")))
                .andExpect(header().doesNotExist("next"))
                .andExpect(header().string("prev", Matchers.containsString("/books?query=The&page=1&size=2")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("The Odyssey"));
    }

    @Test
    public void testGetBookByQuery_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("query", "Test Book"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No books found for the given query"));
    }
    @Test
    public void testNoCriteriaProvided() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No search criteria provided"));
    }

    /**
     * Browsing and searching answer with the same paged shape, so the reader can page through
     * search results the way they page through the shelves.
     */
    @Test
    public void testPaginatedNarrowedByQuery() throws Exception {
        mockMvc.perform(get("/books/paginated")
                        .param("page", "0")
                        .param("size", "2")
                        .param("query", "The"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.data[0].title").value("The Catcher in the Rye"))
                .andExpect(jsonPath("$.data[1].title").value("The Divine Comedy"));
    }

    @Test
    public void testPaginatedWithQueryMatchingNothing() throws Exception {
        mockMvc.perform(get("/books/paginated")
                        .param("query", "no-such-book"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No books match that search."));
    }

    /** A blank query browses the shelves rather than searching for nothing. */
    @Test
    public void testPaginatedIgnoresABlankQuery() throws Exception {
        mockMvc.perform(get("/books/paginated")
                        .param("size", "5")
                        .param("query", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("1984"));
    }

    /**
     * The availability flag alone cannot tell "you have this out" from "somebody else does", which
     * is why the panel used to offer Borrow on a book the reader was already holding.
     */
    @Test
    @WithMockUser(username = "member")
    public void testBookDetailSaysWhenTheReaderIsTheBorrower() throws Exception {
        Customer member = customerUseCase.createNewCustomer(
                new CreateNewCustomer("Detail Member", "detail@example.com", true));
        userRepository.save(new UserEntity(
                "member", passwordEncoder.encode("secret"), "USER", member.getCustomerId()));

        Book book = bookUseCase.createNewBook(new CreateNewBook("Borrowed Book", "1234567891",
                2021, List.of(new CreateNewAuthor("Detail Author", "test"))));

        mockMvc.perform(get("/books/{id}", book.getBookId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.borrowedByMe").value(false));

        transactionUseCase.borrowBook(member.getCustomerId(), book.getBookId());

        mockMvc.perform(get("/books/{id}", book.getBookId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.borrowedByMe").value(true))
                .andExpect(jsonPath("$.dueDate").exists());
    }

    /** Somebody else's loan gets the reader a due date to wait for, not a Return button. */
    @Test
    public void testBookDetailSaysWhenSomebodyElseHasIt() throws Exception {
        Customer other = customerUseCase.createNewCustomer(
                new CreateNewCustomer("Other Member", "other@example.com", true));
        Book book = bookUseCase.createNewBook(new CreateNewBook("Someone Elses Book", "1234567893",
                2021, List.of(new CreateNewAuthor("Detail Author", "test"))));

        transactionUseCase.borrowBook(other.getCustomerId(), book.getBookId());

        mockMvc.perform(get("/books/{id}", book.getBookId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.borrowedByMe").value(false))
                .andExpect(jsonPath("$.dueDate").exists());
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
