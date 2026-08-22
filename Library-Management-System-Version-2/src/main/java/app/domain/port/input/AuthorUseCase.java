package app.domain.port.input;

import app.domain.dto.CreateNewAuthor;
import app.domain.model.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** What the application can be asked to do with authors. */
public interface AuthorUseCase {
    /** Creates an author from the submitted details. */
    Author createNewAuthor(CreateNewAuthor createNewAuthor);

    /** The author with this id, or empty when there is none. */
    Optional<Author> findAuthorById(UUID authorId);

    /** The author with exactly this name, or empty. */
    Optional<Author> getAuthorByName(String name);

    /** One page of authors. */
    Page<Author> getPaginatedAuthors(Pageable pageable);

    /** One page of authors matching a free-text query. */
    Page<Author> searchAuthors(String query, Pageable pageable);

    /** Overwrites an author's details. */
    void updateAuthor(UUID authorId, Author author);
}
