package app.domain.port.output;

import app.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Storage the domain needs for books. */
public interface BookRepositoryPort {
    /** Stores a new book. */
    void saveBook(Book book);

    /** Overwrites the stored book with this id. */
    void updateBook(UUID bookID, Book book);

    /** Removes the stored book. */
    void deleteBook(UUID bookId);

    /** The stored book with exactly this title, or empty. */
    Optional<Book> searchBookByTitle(String title);

    /** A stored book by this author, narrowed by availability. */
    Optional<Book> searchBookByAuthors(String author, boolean isAvailable);

    /** The stored book with this ISBN, or empty. */
    Optional<Book> searchByIsbn(String isbn);

    /** The stored book with this id, or empty. */
    Optional<Book> searchBookById(UUID id);

    /** One page of stored books matching a free-text query. */
    Page<Book> searchBooks(String query, Pageable pageable);

    /** One page of the stored catalogue. */
    Page<Book> getPaginatedBooks(Pageable pageable);

}
