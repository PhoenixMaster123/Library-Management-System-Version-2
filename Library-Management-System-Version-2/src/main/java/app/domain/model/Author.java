package app.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Author {
    private UUID authorId;
    private String name;
    private String bio;
    private Set<Book> books = new HashSet<>();

    public Author(String name, String bio) {
        this.name = name;
        this.bio = bio;
    }

    public Author(UUID authorId, String name, String bio) {
        this.authorId = authorId;
        this.name = name;
        this.bio = bio;
    }
    public Author(UUID authorId, String name, String bio, Set<Book> books) {
        this.authorId = authorId;
        this.name = name;
        this.bio = bio;
        this.books = books;
    }

    public Author() {

    }
}
