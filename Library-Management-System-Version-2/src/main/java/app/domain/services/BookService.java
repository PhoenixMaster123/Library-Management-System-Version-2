package app.domain.services;

import app.domain.dto.CreateNewBook;
import app.domain.models.Author;
import app.domain.models.Book;
import app.domain.port.output.BookRepositoryPort;
import app.domain.port.input.AuthorUseCase;
import app.domain.port.input.BookUseCase;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookService implements BookUseCase {

    private final BookRepositoryPort bookRepositoryPort;
    private final AuthorUseCase authorUseCase;

    @Autowired
    public BookService(BookRepositoryPort bookRepositoryPort, AuthorUseCase authorUseCase) {
        this.bookRepositoryPort = bookRepositoryPort;
        this.authorUseCase = authorUseCase;
    }

    @Override
    public Book createNewBook(CreateNewBook bookToCreate) {
        if (bookRepositoryPort.searchBookByTitle(bookToCreate.getTitle()).isPresent()) {
            throw new IllegalArgumentException("Book with the same title already exists.");
        }
        if(bookRepositoryPort.searchByIsbn(bookToCreate.getIsbn()).isPresent()) {
            throw new IllegalArgumentException("Book with the same isbn already exists.");
        }

        Set<Author> authors = bookToCreate.getAuthors().stream()
                .map(authorDto -> authorUseCase.getAuthorByName(authorDto.getName())
                        .orElseGet(() -> authorUseCase.createNewAuthor(authorDto)))
                .collect(Collectors.toSet());

        Book book = new Book(
                bookToCreate.getTitle(),
                bookToCreate.getIsbn(),
                bookToCreate.getPublicationYear(),
                true,
                LocalDate.now()
        );
        book.getAuthors().addAll(authors);

        bookRepositoryPort.saveBook(book);
        return book;
    }

    @Override
    public Page<Book> getPaginatedBooks(Pageable pageable) {
        return bookRepositoryPort.getPaginatedBooks(pageable);
    }

    @Override
    //@Cacheable(value = "book", key = "#title")
    public Optional<Book> searchBookByTitle(String title) {
        return bookRepositoryPort.searchBookByTitle(title);
    }

    @Override
    //@Cacheable(value = "book", key = "#author")
    public Optional<Book> searchBookByAuthors(String author, boolean isAvailable) {
        return bookRepositoryPort.searchBookByAuthors(author, isAvailable);
    }

    @Override
    //@Cacheable(value = "book", key = "#isbn")
    public Optional<Book> searchByIsbn(String isbn) {
        return bookRepositoryPort.searchByIsbn(isbn);
    }

    @Override
    //@Cacheable(value = "book", key = "#id")
    public Optional<Book> searchById(UUID id) {
        return bookRepositoryPort.searchBookById(id);
    }

    @Override
    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookRepositoryPort.searchBooks(query, pageable);
    }

    @Override
    //@CachePut(value = "book", key = "#book.bookId")
    public void updateBook(UUID bookID, Book book) {
        bookRepositoryPort.updateBook(bookID, book);
    }

    @Override
    //@CacheEvict(value = "book", key = "#bookId")
    public void deleteBook(UUID bookId) {
        bookRepositoryPort.deleteBook(bookId);
    }
}
