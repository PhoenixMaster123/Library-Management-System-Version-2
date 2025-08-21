package app.adapters.input;

import app.domain.dto.CreateNewAuthor;
import app.adapters.output.repositories.AuthorRepository;
import app.adapters.output.repositories.BookRepository;
import app.domain.model.Author;
import app.domain.model.Book;
import app.domain.port.input.BookUseCase;
import app.domain.dto.CreateNewBook;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
    @Test
    public void testCreateNewBook() throws Exception {
        CreateNewBook newBook = new CreateNewBook("Test Book", "1234567890",
                2021, List.of(
                        new CreateNewAuthor("Test Author", "test")));

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.isbn").value("1234567890"))
                .andExpect(jsonPath("$.publicationYear").value(2021))
                .andExpect(jsonPath("$.authors[0].name").value("Test Author"));

        assertEquals("Test Book", newBook.getTitle());
        assertEquals("1234567890", newBook.getIsbn());
        assertEquals(2021, newBook.getPublicationYear());
        assertEquals("Test Author", newBook.getAuthors().getFirst().getName());
    }

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
    public void testUpdateBook() throws Exception {
        Book createdBook = bookUseCase.createNewBook(
                new CreateNewBook("Test Book", "1234567890",
                        2021, List.of(
                        new CreateNewAuthor("Test Author", "test"))));

        Book bookToUpdate = new Book();
        bookToUpdate.setTitle("Updated Title");
        bookToUpdate.setIsbn("1234567890");
        bookToUpdate.setPublicationYear(2021);
        bookToUpdate.setAvailable(true);
        bookToUpdate.setCreatedAt(LocalDate.now());
        bookToUpdate.setAuthors(Set.of(new Author("Updated Author", "updated")));

        mockMvc.perform(put("/books/" + createdBook.getBookId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookToUpdate)))
                .andExpect(status().isOk())
                .andExpect(content().string("Book updated successfully"));

        assertEquals("Updated Title", bookToUpdate.getTitle());
        assertEquals("1234567890", bookToUpdate.getIsbn());
        assertEquals(2021, bookToUpdate.getPublicationYear());
        assertTrue(bookToUpdate.isAvailable());
        assertNotNull(bookToUpdate.getCreatedAt());
    }
    @Test
    public void testUpdateBook_NotFound() throws Exception {
        Book bookToUpdate = new Book();
        bookToUpdate.setTitle("Updated Title");
        bookToUpdate.setIsbn("1234567890");
        bookToUpdate.setPublicationYear(2021);
        bookToUpdate.setAvailable(true);
        bookToUpdate.setCreatedAt(LocalDate.now());
        bookToUpdate.setAuthors(Set.of(new Author("Updated Author", "updated")));

        mockMvc.perform(put("/books/12345678-1234-1234-1234-123456789012")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookToUpdate)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Book not found"));
    }

    @Test
    public void testDeleteBook() throws Exception {
        Book book = bookUseCase.createNewBook(
                new CreateNewBook("Test Book", "1234567890",
                        2021, List.of(
                        new CreateNewAuthor("Test Author", "test"))));

        mockMvc.perform(delete("/books/" + book.getBookId()))
                .andExpect(status().isOk())
                .andExpect(content().string("Book successfully deleted!!"));

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
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.isbn").value("1234567890"))
                .andExpect(jsonPath("$.publicationYear").value(2021))
                .andExpect(jsonPath("$.authors[0].name").value("Test Author"));

    }
    @Test
    public void testGetBookById_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("id", "12345678-1234-1234-1234-123456789012"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Book not found"));
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
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.isbn").value("1234567890"))
                .andExpect(jsonPath("$.publicationYear").value(2021))
                .andExpect(jsonPath("$.authors[0].name").value("Test Author"));
    }
    @Test
    public void testGetBookByTitle_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("title", "Test Book"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Book with the given title not found"));
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
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.isbn").value("1234567890"))
                .andExpect(jsonPath("$.publicationYear").value(2021))
                .andExpect(jsonPath("$.authors[0].name").value("Test Author"));
    }
    @Test
    public void testGetBookByIsbn_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("isbn", "1234567890"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Book with the given ISBN not found"));
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
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.isbn").value("1234567890"))
                .andExpect(jsonPath("$.publicationYear").value(2021))
                .andExpect(jsonPath("$.authors[0].name").value("Test Author"));
    }
    @Test
    public void testGetBookByAuthor_NotFound() throws Exception {
        mockMvc.perform(get("/books")
                        .param("author", "Test Author"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No books found by the given author"));
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
                .andExpect(jsonPath("$.length()").value(2))  // Only 2 books in the first page
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
                .andExpect(content().string("No books found for the given query"));
    }
    @Test
    public void testNoCriteriaProvided() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No search criteria provided"));
    }
    @AfterEach
    public void tearDown() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}
