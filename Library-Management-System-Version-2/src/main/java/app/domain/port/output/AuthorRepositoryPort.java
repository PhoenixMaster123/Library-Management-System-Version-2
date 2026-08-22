package app.domain.port.output;

import app.domain.model.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Storage the domain needs for authors. */
public interface AuthorRepositoryPort {
    /** Stores a new author. */
    void saveAuthor(Author author);

    /** Overwrites the stored author with this id. */
    void updateAuthor(UUID authorId, Author author);

    /** Removes the stored author. */
    void deleteAuthor(UUID id);

    /** The stored author with exactly this name, or empty. */
    Optional<Author> searchAuthorByName(String name);

    /** The stored author with this id, or empty. */
    Optional<Author> searchAuthorByID(UUID id);

    /** One page of stored authors. */
    Page<Author> getPaginatedAuthors(Pageable pageable);

    /** One page of stored authors matching a free-text query. */
    Page<Author> searchAuthors(String query, Pageable pageable);
}
