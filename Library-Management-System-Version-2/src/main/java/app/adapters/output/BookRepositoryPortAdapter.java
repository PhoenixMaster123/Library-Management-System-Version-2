package app.adapters.output;

import app.adapters.output.entity.AuthorEntity;
import app.adapters.output.entity.BookEntity;
import app.adapters.output.repositories.AuthorRepository;
import app.adapters.output.repositories.BookRepository;
import app.adapters.output.mapper.EntityMapper;
import app.domain.model.Book;
import app.domain.port.output.BookRepositoryPort;
import app.infrastructure.exceptions.BookNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Locale;

/** Persists books through JPA. Transactional at class level: a save also resolves and saves authors. */
@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookRepositoryPortAdapter implements BookRepositoryPort {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    /** Stores a book, creating or reusing each of its authors. */
    @Override
    public void saveBook(Book book) {
        log.info("Saving new book: {}", book.getTitle());
        Set<AuthorEntity> authorEntities = book.getAuthors().stream()
                .map(author -> {
                    Optional<AuthorEntity> existingAuthor = authorRepository.findByName(author.getName());
                    return existingAuthor.orElseGet(() -> {
                        AuthorEntity newAuthorEntity = new AuthorEntity(
                                UUID.randomUUID(),
                                author.getName(),
                                author.getBio(),
                                new HashSet<>()
                        );
                        log.info("Creating new author: {}", author.getName());
                        return authorRepository.save(newAuthorEntity);
                    });
                })
                .collect(Collectors.toSet());

        BookEntity bookEntity = BookEntity.builder()
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .publicationYear(book.getPublicationYear())
                .availability(book.isAvailable())
                .description(book.getDescription())
                .createdAt(book.getCreatedAt())
                .build();

        authorEntities.forEach(author -> {
            if (author.getBooks() == null) {
                author.setBooks(new HashSet<>());
            }
            author.getBooks().add(bookEntity);
        });
        BookEntity savedEntity = bookRepository.save(bookEntity);
        book.setBookId(savedEntity.getBookId());
        log.info("Book saved with ID: {}", savedEntity.getBookId());
    }

    /** Overwrites a stored book's details. */
    @Override
    public void updateBook(UUID bookID, Book newBook) {
        log.info("Updating book with ID: {}", bookID);
        bookRepository.findById(bookID).ifPresentOrElse(entity -> {
            entity.setTitle(newBook.getTitle());
            entity.setIsbn(newBook.getIsbn());
            entity.setPublicationYear(newBook.getPublicationYear());
            entity.setAvailability(newBook.isAvailable());
            entity.setDescription(newBook.getDescription());
            entity.setCreatedAt(newBook.getCreatedAt());
            bookRepository.save(entity);
            log.info("Book updated: {}", entity.getTitle());
        }, () -> log.warn("Book with ID {} not found. Update skipped.", bookID));
    }

    /** Removes a stored book. */
    @Override
    public void deleteBook(UUID bookID) {
        log.info("Deleting book with ID: {}", bookID);
        Optional<BookEntity> existingBook = bookRepository.findById(bookID);
        if (existingBook.isPresent()) {
            BookEntity book = existingBook.get();

            for (AuthorEntity author : book.getAuthors()) {
                author.getBooks().remove(book);
            }

            book.setAuthors(new HashSet<>());

            bookRepository.save(book);

            bookRepository.deleteById(bookID);
            log.info("Book deleted successfully: {}", bookID);
        } else {
            log.error("Book not found with ID: {}", bookID);
            throw new BookNotFoundException("Book not found with ID: " + bookID);
        }
    }

    /** One page of the stored catalogue. */
    @Override
    public Page<Book> getPaginatedBooks(Pageable pageable) {
        return bookRepository.findAll(pageable).map(EntityMapper::toBook);
    }

    /** The stored book with exactly this title, or empty. */
    @Override
    public Optional<Book> searchBookByTitle(String title) {
        return bookRepository.findBookByTitle(title).map(EntityMapper::toBook);
    }

    /** A stored book by this author, narrowed by availability. */
    @Override
    public Optional<Book> searchBookByAuthors(String author, boolean isAvailable) {
        return bookRepository.findBooksByAuthor(author, isAvailable).stream()
                .map(EntityMapper::toBook)
                .findFirst();
    }

    /** The stored book with this ISBN, or empty. */
    @Override
    public Optional<Book> searchByIsbn(String isbn) {
        return bookRepository.findBooksByIsbn(isbn).map(EntityMapper::toBook);
    }

    /** The stored book with this id, or empty. */
    @Override
    public Optional<Book> searchBookById(UUID id) {
        return bookRepository.findBookByBookId(id).map(EntityMapper::toBook);
    }

    /** One page of stored books matching a free-text query. */
    @Override
    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookRepository.findBooksByQuery(query.toLowerCase(Locale.ROOT), pageable).map(EntityMapper::toBook);
    }
}
