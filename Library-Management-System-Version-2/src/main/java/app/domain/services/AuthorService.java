package app.domain.services;

import app.domain.dto.CreateNewAuthor;
import app.domain.model.Author;
import app.domain.port.output.AuthorRepositoryPort;
import app.domain.port.input.AuthorUseCase;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthorService implements AuthorUseCase {

    private final AuthorRepositoryPort authorRepositoryPort;

    @Autowired
    public AuthorService(AuthorRepositoryPort authorRepositoryPort) {
        this.authorRepositoryPort = authorRepositoryPort;
    }

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

    @Override
    //@CachePut(value = "author", key = "#author.authorId")
    public void updateAuthor(UUID authorId, Author author) {
        authorRepositoryPort.updateAuthor(authorId, author);
    }

    @Override
    //@Cacheable(value = "author", key = "#name")
    public Optional<Author> getAuthorByName(String name) {
        return authorRepositoryPort.searchAuthorByName(name);
    }

    @Override
    public Page<Author> getPaginatedAuthors(Pageable pageable) {
        return authorRepositoryPort.getPaginatedAuthors(pageable);
    }

    @Override
    public Page<Author> searchAuthors(String query, Pageable pageable) {
        return authorRepositoryPort.searchAuthors(query, pageable);
    }

    @Override
    //@Cacheable(value = "author", key = "#authorId")
    public Optional<Author> findAuthorById(UUID authorId) {
        return authorRepositoryPort.searchAuthorByID(authorId);
    }
}
