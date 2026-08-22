package app.domain.services;

import app.domain.dto.CreateNewBook;
import app.domain.model.Author;
import app.domain.model.Book;
import app.domain.port.input.AuthorUseCase;
import app.domain.port.input.BookUseCase;
import app.domain.port.output.BookRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** The catalogue: adding, editing and searching books. */
@Service
@Transactional
@RequiredArgsConstructor
public class BookService implements BookUseCase {

    private final BookRepositoryPort bookRepositoryPort;
    private final AuthorUseCase authorUseCase;

    /** Adds a book, rejecting a duplicate title or ISBN and reusing authors already on file. */
    @Override
    public Book createNewBook(CreateNewBook bookToCreate) {
        if (bookRepositoryPort.searchBookByTitle(bookToCreate.getTitle()).isPresent()) {
            throw new IllegalArgumentException("Book with the same title already exists.");
        }
        if (bookRepositoryPort.searchByIsbn(bookToCreate.getIsbn()).isPresent()) {
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
        book.setDescription(bookToCreate.getDescription());
        book.getAuthors().addAll(authors);

        bookRepositoryPort.saveBook(book);
        return book;
    }

    /** One page of the catalogue. */
    @Override
    public Page<Book> getPaginatedBooks(Pageable pageable) {
        return bookRepositoryPort.getPaginatedBooks(pageable);
    }

    /** The book with exactly this title, or empty. */
    @Override
    public Optional<Book> searchBookByTitle(String title) {
        return bookRepositoryPort.searchBookByTitle(title);
    }

    /** A book by this author, narrowed by availability. */
    @Override
    public Optional<Book> searchBookByAuthors(String author, boolean isAvailable) {
        return bookRepositoryPort.searchBookByAuthors(author, isAvailable);
    }

    /** The book with this ISBN, or empty. */
    @Override
    public Optional<Book> searchByIsbn(String isbn) {
        return bookRepositoryPort.searchByIsbn(isbn);
    }

    /** The book with this id, or empty. */
    @Override
    public Optional<Book> searchById(UUID id) {
        return bookRepositoryPort.searchBookById(id);
    }

    /** One page of books matching a free-text query. */
    @Override
    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookRepositoryPort.searchBooks(query, pageable);
    }

    /** Overwrites a book's details. */
    @Override
    public void updateBook(UUID bookID, Book book) {
        bookRepositoryPort.updateBook(bookID, book);
    }

    /** Removes a book from the catalogue. */
    @Override
    public void deleteBook(UUID bookId) {
        bookRepositoryPort.deleteBook(bookId);
    }
}
