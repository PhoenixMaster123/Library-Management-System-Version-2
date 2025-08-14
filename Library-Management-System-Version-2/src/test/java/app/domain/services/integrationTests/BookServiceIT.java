package app.domain.services.integrationTests;

import app.adapters.in.dto.CreateNewAuthor;
import app.adapters.in.dto.CreateNewBook;
import app.adapters.out.H2.repositories.AuthorRepository;
import app.adapters.out.H2.repositories.BookRepository;
import app.domain.models.Book;
import app.domain.port.BookDao;
import app.domain.services.BookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
public class BookServiceIT {
    @Autowired
    private BookDao realBookDao;
    @Autowired
    private BookService realBookService;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;

    @Test
    void createNewBook_IntegrationTest() {
        CreateNewBook newBook = new CreateNewBook(
                "Spring Boot in Action",
                "9876543210",
                2021,
                List.of(new CreateNewAuthor("Craig Walls", "example"))
        );

        Book createdBook = realBookService.createNewBook(newBook);

        assertNotNull(createdBook.getBookId());
        assertEquals("Spring Boot in Action", createdBook.getTitle());
        assertTrue(realBookDao.searchBookByTitle("Spring Boot in Action").isPresent());
    }
    @Test
    void updateBook_IntegrationTest() {
        Book originalBook = new Book(
                "Original Title", "123-456-789",
                2021, true, LocalDate.now());
        realBookDao.addBook(originalBook);

        Book updatedBook = new Book("Updated Title", "123-456-789", 2022, false, LocalDate.now());
        UUID bookID = originalBook.getBookId();

        realBookService.updateBook(bookID, updatedBook);

        Optional<Book> result = realBookDao.searchBookById(bookID);

        assertTrue(result.isPresent());
        assertEquals("Updated Title", result.get().getTitle());
        assertEquals(2022, result.get().getPublicationYear());
        assertFalse(result.get().isAvailable());
    }
    @Test
    void deleteBook_IntegrationTest() {
        Book book = new Book("Title", "ISBN", 2021,
                true, LocalDate.now());
        realBookDao.addBook(book);

        realBookService.deleteBook(book.getBookId());

        Optional<Book> result = realBookDao.searchBookById(book.getBookId());
        assertTrue(result.isEmpty());
    }
    @Test
    void searchBookByTitle_IntegrationTest() {
        String title = "Unique Title";
        Book book = new Book(title, "ISBN", 2021, true,
                LocalDate.now());

        realBookDao.addBook(book);

        Optional<Book> result = realBookService.searchBookByTitle(title);

        assertTrue(result.isPresent());
        assertEquals(title, result.get().getTitle());
    }
    @Test
    void searchByIsbn_IntegrationTest() {
        String isbn = "123-456-789";
        Book book = new Book("Title", isbn, 2021,
                true, LocalDate.now());

        realBookDao.addBook(book);

        Optional<Book> result = realBookService.searchByIsbn(isbn);

        assertTrue(result.isPresent());
        assertEquals(isbn, result.get().getIsbn());
    }

    @Test
    void searchById_IntegrationTest() {
        Book book = new Book("Title", "ISBN", 2021,
                true, LocalDate.now());
        realBookDao.addBook(book);

        Optional<Book> result = realBookService.searchById(book.getBookId());

        assertTrue(result.isPresent());
        assertEquals("Title", result.get().getTitle());
    }

    @Test
    void searchBooks_IntegrationTest() {
        String query = "search";
        Book book1 = new Book("Search Result 1", "ISBN1", 2021, true, LocalDate.now());
        Book book2 = new Book("Search Result 2", "ISBN2", 2020, false, LocalDate.now());

        realBookDao.addBook(book1);
        realBookDao.addBook(book2);

        Page<Book> result = realBookService.searchBooks(query, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getPaginatedBooks_IntegrationTest() {
        Pageable pageable = PageRequest.of(0, 10);
        Book book1 = new Book("Title1", "ISBN1", 2021, true, LocalDate.now());
        Book book2 = new Book("Title2", "ISBN2", 2020, false, LocalDate.now());

        realBookDao.addBook(book1);
        realBookDao.addBook(book2);

        Page<Book> result = realBookService.getPaginatedBooks(pageable);

        assertEquals(12, result.getTotalElements());
    }
    @AfterEach
    void tearDown() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}
