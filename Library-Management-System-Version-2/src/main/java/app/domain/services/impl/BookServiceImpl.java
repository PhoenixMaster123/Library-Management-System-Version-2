package app.domain.services.impl;

import app.adapters.in.dto.CreateNewBook;
import app.domain.models.Author;
import app.domain.models.Book;
import app.domain.port.BookDao;
import app.domain.services.AuthorService;
import app.domain.services.BookService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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
public class BookServiceImpl implements BookService {

    private final BookDao bookDao;
    private final AuthorService authorService;

    @Autowired
    public BookServiceImpl(BookDao bookDao, AuthorService authorService) {
        this.bookDao = bookDao;
        this.authorService = authorService;
    }

    public Book createNewBook(CreateNewBook bookToCreate) {
        if (bookDao.searchBookByTitle(bookToCreate.getTitle()).isPresent()) {
            throw new IllegalArgumentException("Book with the same title already exists.");
        }
        if(bookDao.searchByIsbn(bookToCreate.getIsbn()).isPresent()) {
            throw new IllegalArgumentException("Book with the same isbn already exists.");
        }

        Set<Author> authors = bookToCreate.getAuthors().stream()
                .map(authorDto -> authorService.getAuthorByName(authorDto.getName())
                        .orElseGet(() -> authorService.createNewAuthor(authorDto)))
                .collect(Collectors.toSet());

        Book book = new Book(
                bookToCreate.getTitle(),
                bookToCreate.getIsbn(),
                bookToCreate.getPublicationYear(),
                true,
                LocalDate.now()
        );
        book.getAuthors().addAll(authors);

        bookDao.addBook(book);
        return book;
    }

    public Page<Book> getPaginatedBooks(Pageable pageable) {
        return bookDao.getPaginatedBooks(pageable);
    }

    public Optional<Book> searchBookByTitle(String title) {
        return bookDao.searchBookByTitle(title);
    }

    public Optional<Book> searchBookByAuthors(String author, boolean isAvailable) {
        return bookDao.searchBookByAuthors(author, isAvailable);
    }

    public Optional<Book> searchByIsbn(String isbn) {
        return bookDao.searchByIsbn(isbn);
    }

    public Optional<Book> searchById(UUID id) {
        return bookDao.searchBookById(id);
    }

    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookDao.searchBooks(query, pageable);
    }

    public void updateBook(UUID bookID, Book book) {
        bookDao.updateBook(bookID, book);
    }

    public void deleteBook(UUID bookId) {
        bookDao.deleteBook(bookId);
    }
}
