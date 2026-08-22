package app.domain.services;

import app.domain.dto.CreateNewAuthor;
import app.domain.model.Author;
import app.domain.port.input.AuthorUseCase;
import app.domain.port.output.AuthorRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** Author records and the books attributed to them. */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorService implements AuthorUseCase {

    private final AuthorRepositoryPort authorRepositoryPort;

    /** Adds an author, rejecting a name the catalogue already holds. */
    @Override
    public Author createNewAuthor(CreateNewAuthor createNewAuthor) {
        if (authorRepositoryPort.searchAuthorByName(createNewAuthor.getName()).isPresent()) {
            throw new IllegalArgumentException("Author with the same name already exists.");
        }
        Author author = new Author(createNewAuthor.getName(), createNewAuthor.getBio());
        authorRepositoryPort.saveAuthor(author);
        return authorRepositoryPort.searchAuthorByName(createNewAuthor.getName())
                .orElseThrow(() -> new IllegalStateException("Author was not properly saved"));
    }

    /** Overwrites an author's details. */
    @Override
    public void updateAuthor(UUID authorId, Author author) {
        authorRepositoryPort.updateAuthor(authorId, author);
    }

    /** The author with exactly this name, or empty. */
    @Override
    public Optional<Author> getAuthorByName(String name) {
        return authorRepositoryPort.searchAuthorByName(name);
    }

    /** One page of authors. */
    @Override
    public Page<Author> getPaginatedAuthors(Pageable pageable) {
        return authorRepositoryPort.getPaginatedAuthors(pageable);
    }

    /** One page of authors matching a free-text query. */
    @Override
    public Page<Author> searchAuthors(String query, Pageable pageable) {
        return authorRepositoryPort.searchAuthors(query, pageable);
    }

    /** The author with this id, or empty. */
    @Override
    public Optional<Author> findAuthorById(UUID authorId) {
        return authorRepositoryPort.searchAuthorByID(authorId);
    }
}
