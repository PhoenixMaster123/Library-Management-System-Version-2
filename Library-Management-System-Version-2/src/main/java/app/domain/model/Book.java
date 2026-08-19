package app.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A book on the shelves, and whether it is currently available. */
@Getter
@Setter
@NoArgsConstructor
public class Book {
    private UUID bookId;
    private String title;
    private String isbn;
    private int publicationYear;
    private boolean isAvailable;
    private LocalDate createdAt;
    private Set<Author> authors = new HashSet<>();

    /**
     * Free text shown on the book's detail panel, usually filled in from the catalogue lookup.
     * Set separately rather than through a constructor, which is why there is no @AllArgsConstructor
     * here: every existing call site builds a book without one.
     */
    private String description;

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

    public Book(UUID bookId, String title, String isbn, int publicationYear, boolean isAvailable,
                LocalDate createdAt, Set<Author> authors) {
        this(bookId, title, isbn, publicationYear, isAvailable, createdAt);
        this.authors = authors;
    }
}
