package app.domain.port.input;

import app.domain.dto.CreateNewBook;
import app.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;


/** What the application can be asked to do with the catalogue. */
public interface BookUseCase {
    /** Adds a book to the catalogue and returns it with its assigned id. */
    Book createNewBook(CreateNewBook bookToCreate);

    /** One page of the catalogue. */
    Page<Book> getPaginatedBooks(Pageable pageable);

    /** One page of books matching a free-text query. */
    Page<Book> searchBooks(String query, Pageable pageable);

    /** The book with exactly this title, or empty. */
    Optional<Book> searchBookByTitle(String title);

    /** A book by this author, narrowed by whether it is on the shelf. */
    Optional<Book> searchBookByAuthors(String author, boolean isAvailable);

    /** The book with this ISBN, or empty. */
    Optional<Book> searchByIsbn(String isbn);

    /** The book with this id, or empty. */
    Optional<Book> searchById(UUID id);

    /** Overwrites a book's details. */
    void updateBook(UUID bookId, Book book);

    /** Removes a book from the catalogue. */
    void deleteBook(UUID bookId);
}
