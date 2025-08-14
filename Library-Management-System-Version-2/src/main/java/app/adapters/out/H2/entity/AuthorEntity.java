package app.adapters.out.H2.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "authors")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID authorId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String bio;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "author_books", // Specify the join table name
            joinColumns = @JoinColumn(name = "author_id"), // Foreign key to authors
            inverseJoinColumns = @JoinColumn(name = "book_id") // Foreign key to books
    )
    private Set<BookEntity> books = new HashSet<>();

    public AuthorEntity() {

    }
}
