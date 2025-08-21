package app.domain.port.output;

import app.domain.model.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AuthorRepositoryPort {
    void saveAuthor(Author author);
    void updateAuthor(UUID authorId, Author author);
    void deleteAuthor(UUID id);
    Optional<Author> searchAuthorByName(String name);
    Optional<Author> searchAuthorByID(UUID id);
    Page<Author> getPaginatedAuthors(Pageable pageable);
    Page<Author> searchAuthors(String query, Pageable pageable);
}
