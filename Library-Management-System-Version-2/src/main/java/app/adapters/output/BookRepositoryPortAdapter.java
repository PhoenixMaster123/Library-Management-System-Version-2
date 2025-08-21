package app.adapters.output;

import app.adapters.output.entity.AuthorEntity;
import app.adapters.output.entity.BookEntity;
import app.adapters.output.repositories.AuthorRepository;
import app.adapters.output.repositories.BookRepository;
import app.domain.model.Author;
import app.domain.port.output.BookRepositoryPort;
import app.domain.model.Book;
import app.infrastructure.exceptions.BookNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BookRepositoryPortAdapter implements BookRepositoryPort {
    private static final Logger log = LoggerFactory.getLogger(BookRepositoryPortAdapter.class);

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookRepositoryPortAdapter(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

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
                .created_at(book.getCreatedAt())
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

    @Override
    public void updateBook(UUID bookID, Book newBook) {
        log.info("Updating book with ID: {}", bookID);
        bookRepository.findById(bookID).ifPresentOrElse(entity -> {
            entity.setTitle(newBook.getTitle());
            entity.setIsbn(newBook.getIsbn());
            entity.setPublicationYear(newBook.getPublicationYear());
            entity.setAvailability(newBook.isAvailable());
            entity.setCreated_at(newBook.getCreatedAt());
            bookRepository.save(entity);
            log.info("Book updated: {}", entity.getTitle());
        }, () -> log.warn("Book with ID {} not found. Update skipped.", bookID));
    }

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

    @Override
    public Page<Book> getPaginatedBooks(Pageable pageable) {
        return bookRepository.findAll(pageable).map(this::mapToBook);
    }

    @Override
    public Optional<Book> searchBookByTitle(String title) {
        Optional<BookEntity> bookEntity = bookRepository.findBookByTitle(title);
        return bookEntity.map(this::mapToBook);
    }

    @Override
    public Optional<Book> searchBookByAuthors(String author, boolean isAvailable) {
        List<BookEntity> entities = bookRepository.findBooksByAuthor(author, isAvailable);
        return entities.stream()
                .map(this::mapToBook).findFirst();
    }

    @Override
    public Optional<Book> searchByIsbn(String isbn) {
        Optional<BookEntity> bookEntity = bookRepository.findBooksByIsbn(isbn);
        return bookEntity.map(this::mapToBook);
    }

    @Override
    public Optional<Book> searchBookById(UUID id) {
        Optional<BookEntity> bookEntity = bookRepository.findBookByBookId(id);
        return bookEntity.map(this::mapToBook);
    }
    @Override
    public Page<Book> searchBooks(String query, Pageable pageable) {
        String lowerQuery = query.toLowerCase();
        Page<BookEntity> bookEntities = bookRepository.findBooksByQuery(lowerQuery, pageable);

        return bookEntities.map(this::mapToBook);
    }
    private Book mapToBook(BookEntity bookEntity) {
        return new Book(
                bookEntity.getBookId(),
                bookEntity.getTitle(),
                bookEntity.getIsbn(),
                bookEntity.getPublicationYear(),
                bookEntity.isAvailability(),
                bookEntity.getCreated_at(),
                bookEntity.getAuthors() != null
                        ? bookEntity.getAuthors().stream()
                        .map(authorEntity -> new Author(
                                authorEntity.getAuthorId(),
                                authorEntity.getName(),
                                authorEntity.getBio()
                        ))
                        .collect(Collectors.toSet())
                        : new HashSet<>()
        );
    }

}
