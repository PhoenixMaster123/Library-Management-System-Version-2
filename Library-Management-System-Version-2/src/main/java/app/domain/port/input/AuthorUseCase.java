package app.domain.port.input;

import app.domain.dto.CreateNewAuthor;
import app.domain.model.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AuthorUseCase {
    Author createNewAuthor(CreateNewAuthor createNewAuthor);
    Optional<Author> findAuthorById(UUID authorId);
    Optional<Author> getAuthorByName(String name);
    Page<Author> getPaginatedAuthors(Pageable pageable);
    Page<Author> searchAuthors(String query, Pageable pageable);
    void updateAuthor(UUID authorId, Author author);
}
