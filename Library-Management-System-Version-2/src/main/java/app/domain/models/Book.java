package app.domain.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Book {
    private UUID bookId;
    private String title;
    private String isbn;
    private int publicationYear;
    private boolean isAvailable;
    private LocalDate createdAt;
    private Set<Author> authors = new HashSet<>();

    public Book(String title, String isbn, int publicationYear, boolean isAvailable, LocalDate createdAt) {
        this(null, title, isbn, publicationYear, isAvailable, createdAt);
    }

    public Book(UUID bookId, String title, String isbn, int publicationYear, boolean isAvailable, LocalDate createdAt) {
        this.bookId = bookId;
        this.title = title;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.isAvailable = isAvailable;
        this.createdAt = createdAt;
    }
    public Book(UUID bookId, String title, String isbn, int publicationYear, boolean availability, LocalDate createdAt, Set<Author> authors) {
        this.bookId = bookId;
        this.title = title;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.isAvailable = availability;
        this.createdAt = createdAt;
        this.authors = authors;
    }

    public Book() {
    }
}
