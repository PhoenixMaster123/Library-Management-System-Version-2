package app.adapters.output;

import app.adapters.output.entity.AuthorEntity;
import app.adapters.output.mapper.EntityMapper;
import app.adapters.output.repositories.AuthorRepository;
import app.domain.model.Author;
import app.domain.port.output.AuthorRepositoryPort;
import app.infrastructure.exceptions.AuthorNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;

/** Persists authors through JPA. */
@Component
@RequiredArgsConstructor
@Transactional
public class AuthorRepositoryPortAdapter implements AuthorRepositoryPort {
    private final AuthorRepository authorRepository;

    @Override
    public void saveAuthor(Author author) {
        AuthorEntity authorEntity = AuthorEntity.builder()
                .authorId(author.getAuthorId())
                .name(author.getName())
                .bio(author.getBio())
                .build();
        authorRepository.save(authorEntity);
    }
    @Override
    public Page<Author> getPaginatedAuthors(Pageable pageable) {
        Page<AuthorEntity> authorEntities = authorRepository.findAllAuthorsWithBooks(pageable);

        List<Author> authors = authorEntities.stream()
                .map(EntityMapper::toAuthor)
                .toList();

        return new PageImpl<>(authors, pageable, authorEntities.getTotalElements());
    }

    @Override
    public Page<Author> searchAuthors(String query, Pageable pageable) {
        String queryLowerCase = query.toLowerCase(Locale.ROOT);
        Page<AuthorEntity> authorEntities = authorRepository.
                searchAuthorsByQuery(queryLowerCase, pageable);

        List<Author> authors = authorEntities.stream()
                .map(EntityMapper::toAuthor)
                .toList();

        return new PageImpl<>(authors, pageable, authorEntities.getTotalElements());
    }

    @Override
    public void updateAuthor(UUID authorId, Author newAuthor) {
        AuthorEntity authorEntity = authorRepository.findById(authorId)
                .orElseThrow(() -> new AuthorNotFoundException("Author with ID " + authorId + " not found"));

        authorEntity.setName(newAuthor.getName());
        authorEntity.setBio(newAuthor.getBio());
        authorRepository.save(authorEntity);
    }

    @Override
    public void deleteAuthor(UUID id) {
        authorRepository.deleteById(id);
    }

    @Override
    public Optional<Author> searchAuthorByName(String name) {
        return authorRepository.findByName(name).map(EntityMapper::toAuthor);
    }

    @Override
    public Optional<Author> searchAuthorByID(UUID id) {
        return authorRepository.findById(id).map(EntityMapper::toAuthor);
    }
}
