package app.domain.services.unitTests;

import app.domain.dto.CreateNewAuthor;
import app.domain.dto.CreateNewBook;
import app.domain.models.Author;
import app.domain.models.Book;
import app.domain.port.output.BookRepositoryPort;
import app.domain.port.input.AuthorUseCase;
import app.domain.port.input.BookUseCase;
import app.domain.services.BookService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("unit")
class BookUseCaseTest {
    private BookRepositoryPort mockedBookRepositoryPort;
    private AuthorUseCase mockedAuthorUseCase;

    private BookUseCase bookUseCase;

    @BeforeAll
    void setup() {
        mockedBookRepositoryPort = mock(BookRepositoryPort.class);
        mockedAuthorUseCase = mock(AuthorUseCase.class);
        bookUseCase = new BookService(mockedBookRepositoryPort, mockedAuthorUseCase);
    }

        @Test
        void createNewBook_Success() {
            CreateNewBook newBook = new CreateNewBook(
                    "Effective Java",
                    "123456789",
                    2018,
                    List.of(new CreateNewAuthor("Joshua Bloch", "example"))
            );

            when(mockedBookRepositoryPort.searchBookByTitle("Effective Java")).thenReturn(Optional.empty());
            when(mockedBookRepositoryPort.searchByIsbn("123456789")).thenReturn(Optional.empty());
            when(mockedAuthorUseCase.getAuthorByName("Joshua Bloch")).thenReturn(Optional.empty());
            when(mockedAuthorUseCase.createNewAuthor(any())).thenReturn(new Author("Joshua Bloch", "example"));

            bookUseCase.createNewBook(newBook);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(mockedBookRepositoryPort).saveBook(captor.capture());
            Book createdBook = captor.getValue();

            assertEquals("Effective Java", createdBook.getTitle());
            assertEquals("123456789", createdBook.getIsbn());
            assertEquals(2018, createdBook.getPublicationYear());
            assertTrue(createdBook.isAvailable());
            assertEquals(1, createdBook.getAuthors().size());
        }

        @Test
        void createNewBook_ThrowsException_WhenTitleExists() {
            CreateNewBook newBook = new CreateNewBook(
                    "Effective Java",
                    "123456789",
                    2018,
                    List.of(new CreateNewAuthor("Joshua Bloch", "example"))
            );

            when(mockedBookRepositoryPort.searchBookByTitle("Effective Java")).thenReturn(Optional.of(new Book()));

            assertThrows(IllegalArgumentException.class, () -> bookUseCase.createNewBook(newBook));
        }

        @Test
        void createNewBook_ThrowsException_WhenIsbnExists() {
            CreateNewBook newBook = new CreateNewBook(
                    "Effective Java",
                    "123456789",
                    2018,
                    List.of(new CreateNewAuthor("Joshua Bloch", "example"))
            );

            when(mockedBookRepositoryPort.searchBookByTitle("Effective Java")).thenReturn(Optional.empty());
            when(mockedBookRepositoryPort.searchByIsbn("123456789")).thenReturn(Optional.of(new Book()));

            assertThrows(IllegalArgumentException.class, () -> bookUseCase.createNewBook(newBook));
        }

        @Test
        void deleteBook_Success() {
            UUID bookId = UUID.randomUUID();

            doNothing().when(mockedBookRepositoryPort).deleteBook(bookId);

            bookUseCase.deleteBook(bookId);

            verify(mockedBookRepositoryPort, times(1)).deleteBook(bookId);
        }

        @Test
        void searchBookByTitle_Found() {
            String title = "Sample Title";
            Book book = new Book(title, "ISBN1", 2021, true, null);

            when(mockedBookRepositoryPort.searchBookByTitle(title)).thenReturn(Optional.of(book));

            Optional<Book> result = bookUseCase.searchBookByTitle(title);

            assertTrue(result.isPresent());
            assertEquals(title, result.get().getTitle());
            verify(mockedBookRepositoryPort, times(1)).searchBookByTitle(title);
        }

        @Test
        void searchBookByAuthors_Found() {
            String author = "John Doe";
            boolean isAvailable = true;
            Book book = new Book("Title", "ISBN1", 2021, isAvailable, null);

            when(mockedBookRepositoryPort.searchBookByAuthors(author, isAvailable)).thenReturn(Optional.of(book));

            Optional<Book> result = bookUseCase.searchBookByAuthors(author, isAvailable);

            assertTrue(result.isPresent());
            assertEquals("Title", result.get().getTitle());
            verify(mockedBookRepositoryPort, times(1)).searchBookByAuthors(author, isAvailable);
        }

        @Test
        void searchByIsbn_Found() {
            String isbn = "123-456-789";
            Book book = new Book("Title", isbn, 2021, true, null);

            when(mockedBookRepositoryPort.searchByIsbn(isbn)).thenReturn(Optional.of(book));

            Optional<Book> result = bookUseCase.searchByIsbn(isbn);

            assertTrue(result.isPresent());
            assertEquals(isbn, result.get().getIsbn());
            verify(mockedBookRepositoryPort, times(1)).searchByIsbn(isbn);
        }

        @Test
        void searchById_Found() {
            UUID id = UUID.randomUUID();
            Book book = new Book("Title", "ISBN", 2021, true, null);

            when(mockedBookRepositoryPort.searchBookById(id)).thenReturn(Optional.of(book));

            Optional<Book> result = bookUseCase.searchById(id);

            assertTrue(result.isPresent());
            assertEquals("Title", result.get().getTitle());
            verify(mockedBookRepositoryPort, times(1)).searchBookById(id);
        }

        @Test
        void searchBooks_Found() {
            String query = "search query";
            Pageable pageable = PageRequest.of(0, 10);
            List<Book> books = Arrays.asList(new Book("Title1", "ISBN1", 2021, true, null),
                    new Book("Title2", "ISBN2", 2020, false, null));
            Page<Book> page = new PageImpl<>(books, pageable, books.size());

            when(mockedBookRepositoryPort.searchBooks(query, pageable)).thenReturn(page);

            Page<Book> result = bookUseCase.searchBooks(query, pageable);

            assertEquals(2, result.getTotalElements());
            verify(mockedBookRepositoryPort, times(1)).searchBooks(query, pageable);
        }

        @Test
        void updateBook_Success() {
            UUID bookId = UUID.randomUUID();
            Book bookToUpdate = new Book("Updated Title", "111222333", 2021, true, LocalDate.now());

            bookUseCase.updateBook(bookId, bookToUpdate);

            verify(mockedBookRepositoryPort).updateBook(bookId, bookToUpdate);
        }

        @Test
        void getPaginatedBooks_ReturnsPaginatedResults() {
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<Book> paginatedBooks = new PageImpl<>(List.of(new Book("Title", "ISBN", 2020, true, LocalDate.now())));

            when(mockedBookRepositoryPort.getPaginatedBooks(pageRequest)).thenReturn(paginatedBooks);

            Page<Book> result = bookUseCase.getPaginatedBooks(pageRequest);

            assertEquals(1, result.getTotalElements());
            verify(mockedBookRepositoryPort).getPaginatedBooks(pageRequest);
        }
    }
