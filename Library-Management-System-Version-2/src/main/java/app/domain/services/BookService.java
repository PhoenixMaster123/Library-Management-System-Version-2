package app.domain.services;

import app.domain.models.Author;
import app.domain.port.BookDao;
import app.adapters.in.dto.CreateNewBook;
import app.domain.models.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;


public interface BookService {
    Book createNewBook(CreateNewBook bookToCreate);
    Page<Book> getPaginatedBooks(Pageable pageable);
    Page<Book> searchBooks(String query, Pageable pageable);
    Optional<Book> searchBookByTitle(String title);
    Optional<Book> searchBookByAuthors(String author, boolean isAvailable);
    Optional<Book> searchByIsbn(String isbn);
    Optional<Book> searchById(UUID id);
    void updateBook(UUID bookId, Book book);
    void deleteBook(UUID bookId);
}
