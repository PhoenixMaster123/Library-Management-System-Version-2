package app.adapters.input;

import app.adapters.output.repositories.AuthorRepository;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.repositories.CustomerRepository;
import app.adapters.output.repositories.TransactionRepository;
import app.domain.dto.CreateNewAuthor;
import app.domain.dto.CreateNewBook;
import app.domain.dto.CreateNewCustomer;
import app.domain.model.Author;
import app.domain.model.Book;
import app.domain.model.Customer;
import app.domain.port.input.AuthorUseCase;
import app.domain.port.input.BookUseCase;
import app.domain.port.input.CustomerUseCase;
import app.domain.port.input.TransactionUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
@Tag("integration")
class AdminControllerTestIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BookUseCase bookUseCase;
    @Autowired
    private AuthorUseCase authorUseCase;
    @Autowired
    private CustomerUseCase customerUseCase;
    @Autowired
    private TransactionUseCase transactionUseCase;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    private Book existingBook() {
        return bookUseCase.createNewBook(new CreateNewBook("Test Book", "1234567890", 2021,
                List.of(new CreateNewAuthor("Test Author", "test"))));
    }

    private Book bookPayload(String title) {
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn("1234567890");
        book.setPublicationYear(2021);
        book.setAvailable(true);
        book.setCreatedAt(LocalDate.now());
        book.setAuthors(Set.of(new Author("Updated Author", "updated")));
        return book;
    }

    @Test
    void createBook() throws Exception {
        CreateNewBook newBook = new CreateNewBook("Test Book", "1234567890", 2021,
                List.of(new CreateNewAuthor("Test Author", "test")));

        mockMvc.perform(post("/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.isbn").value("1234567890"))
                .andExpect(jsonPath("$.publicationYear").value(2021))
                .andExpect(jsonPath("$.authors[0].name").value("Test Author"));
    }

    @Test
    void createBook_rejectsInvalidPayload() throws Exception {
        CreateNewBook newBook = new CreateNewBook("", "", 1, List.of());

        mockMvc.perform(post("/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBook)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void updateBook() throws Exception {
        Book created = existingBook();

        mockMvc.perform(put("/admin/books/" + created.getBookId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookPayload("Updated Title"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book updated successfully"));

        assertTrue(bookRepository.findById(created.getBookId())
                .filter(entity -> "Updated Title".equals(entity.getTitle()))
                .isPresent());
    }

    @Test
    void updateBook_notFound() throws Exception {
        mockMvc.perform(put("/admin/books/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookPayload("Updated Title"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found"));
    }

    @Test
    void deleteBook() throws Exception {
        Book book = existingBook();

        mockMvc.perform(delete("/admin/books/" + book.getBookId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book successfully deleted!"));
    }

    @Test
    void deleteBook_notFound() throws Exception {
        mockMvc.perform(delete("/admin/books/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createAuthor() throws Exception {
        mockMvc.perform(post("/admin/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateNewAuthor("Test Author", "test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Author"))
                .andExpect(jsonPath("$.bio").value("test"));
    }

    @Test
    void updateAuthor() throws Exception {
        Author author = authorUseCase.createNewAuthor(new CreateNewAuthor("Test Author", "test"));

        mockMvc.perform(put("/admin/authors/" + author.getAuthorId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Author("Updated Author", "updated"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Author updated successfully!"));

        assertTrue(authorRepository.findById(author.getAuthorId())
                .filter(entity -> "Updated Author".equals(entity.getName()))
                .isPresent());
    }

    /** Used to answer 500: AuthorNotFoundException matched nothing in the advice. */
    @Test
    void updateAuthor_notFound() throws Exception {
        mockMvc.perform(put("/admin/authors/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Author("Updated Author", "updated"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void loans_listsWhoHasWhatOut() throws Exception {
        Book book = existingBook();
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer("Borrower", "borrower@example.com", true));
        transactionUseCase.borrowBook(customer.getCustomerId(), book.getBookId());

        mockMvc.perform(get("/admin/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].customer.name").value("Borrower"))
                .andExpect(jsonPath("$.data[0].book.title").value("Test Book"))
                .andExpect(jsonPath("$.data[0].dueDate").exists())
                .andExpect(jsonPath("$.data[0].returnDate").doesNotExist());
    }

    /** Returned loans drop out of the active view but stay in the full history. */
    @Test
    void loans_activeOnlyExcludesReturned() throws Exception {
        Book book = existingBook();
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer("Borrower", "borrower@example.com", true));
        transactionUseCase.borrowBook(customer.getCustomerId(), book.getBookId());
        transactionUseCase.returnBook(book.getBookId());

        mockMvc.perform(get("/admin/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));

        // The seeded history is in here too, so this only has to be non-empty.
        mockMvc.perform(get("/admin/loans").param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(Matchers.greaterThan(0)));
    }

    /** The catalogue must start with every copy on the shelf: nothing is out until someone borrows. */
    @Test
    void seededCatalogueStartsWithNoOutstandingLoans() throws Exception {
        mockMvc.perform(get("/admin/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    /** The whole point of the split: a signed-in member cannot change the catalogue. */
    @Test
    @WithMockUser(username = "member", roles = "USER")
    void membersAreRefused() throws Exception {
        mockMvc.perform(get("/admin/loans"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/admin/books/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateNewBook("X", "Y", 2021,
                                List.of(new CreateNewAuthor("A", "b"))))))
                .andExpect(status().isForbidden());
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        customerRepository.deleteAll();
    }
}
