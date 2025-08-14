package app.domain.services;

import app.adapters.in.dto.CreateNewAuthor;
import app.domain.models.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AuthorService {
    Author createNewAuthor(CreateNewAuthor createNewAuthor);
    Optional<Author> findAuthorById(UUID authorId);
    Optional<Author> getAuthorByName(String name);
    Page<Author> getPaginatedAuthors(Pageable pageable);
    Page<Author> searchAuthors(String query, Pageable pageable);
    void updateAuthor(UUID authorId, Author author);
}
