package app.domain.port;

import app.domain.models.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface BookDao {
    void addBook(Book book);
    void updateBook(UUID bookID, Book book);
    void deleteBook(UUID bookId);
    Optional<Book> searchBookByTitle(String title);
    Optional<Book> searchBookByAuthors(String author, boolean isAvailable);
    Optional<Book> searchByIsbn(String isbn);
    Optional<Book> searchBookById(UUID id);
    Page<Book> searchBooks(String query, Pageable pageable);
    Page<Book> getPaginatedBooks(Pageable pageable);

}
