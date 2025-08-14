package app.domain.services.impl;

import app.adapters.in.dto.CreateNewAuthor;
import app.domain.models.Author;
import app.domain.port.AuthorDao;
import app.domain.services.AuthorService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthorServiceImpl implements AuthorService {

    private final AuthorDao authorDao;

    @Autowired
    public AuthorServiceImpl(AuthorDao authorDao) {
        this.authorDao = authorDao;
    }

    public Author createNewAuthor(CreateNewAuthor createNewAuthor) {
        if (authorDao.searchAuthorByName(createNewAuthor.getName()).isPresent()) {
            throw new IllegalArgumentException("Author with the same name already exists.");
        }
        Author author = new Author(createNewAuthor.getName(), createNewAuthor.getBio());
        authorDao.addAuthor(author);
        return authorDao.searchAuthorByName(createNewAuthor.getName())
                .orElseThrow(() -> new IllegalStateException("Author was not properly saved"));
    }

    public void updateAuthor(UUID authorId, Author author) {
        authorDao.updateAuthor(authorId, author);
    }

    public Optional<Author> getAuthorByName(String name) {
        return authorDao.searchAuthorByName(name);
    }

    public Page<Author> getPaginatedAuthors(Pageable pageable) {
        return authorDao.getPaginatedAuthors(pageable);
    }

    public Page<Author> searchAuthors(String query, Pageable pageable) {
        return authorDao.searchAuthors(query, pageable);
    }
    public Optional<Author> findAuthorById(UUID authorId) {
        return authorDao.searchAuthorByID(authorId);
    }
}
