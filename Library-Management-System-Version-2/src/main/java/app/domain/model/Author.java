package app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** An author, with the books attributed to them. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Author {
    private UUID authorId;
    private String name;
    private String bio;
    private Set<Book> books = new HashSet<>();

    /** A new author with no id yet; storage assigns one. */
    public Author(String name, String bio) {
        this.name = name;
        this.bio = bio;
    }

    /** An author already known by id, without its books. */
    public Author(UUID authorId, String name, String bio) {
        this.authorId = authorId;
        this.name = name;
        this.bio = bio;
    }
}
