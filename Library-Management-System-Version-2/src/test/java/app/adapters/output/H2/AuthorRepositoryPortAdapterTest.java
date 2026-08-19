package app.adapters.output.H2;

import app.adapters.output.entity.AuthorEntity;
import app.adapters.output.entity.BookEntity;
import app.adapters.output.AuthorRepositoryPortAdapter;
import app.adapters.output.repositories.AuthorRepository;
import app.domain.model.Author;
import app.infrastructure.exceptions.AuthorNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthorRepositoryPortAdapterTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorRepositoryPortAdapter authorDaoAdapter;

    private Author testAuthor;

    @BeforeEach
    void setUp() {
        testAuthor = new Author(UUID.randomUUID(), "Author Name", "Bio", new HashSet<>());
    }

    @Test
    void test_saveAuthor() {
        authorDaoAdapter.saveAuthor(testAuthor);
        ArgumentCaptor<AuthorEntity> captor = ArgumentCaptor.forClass(AuthorEntity.class);
        verify(authorRepository).save(captor.capture());
        AuthorEntity savedAuthorEntity = captor.getValue();
        assertEquals(testAuthor.getName(), savedAuthorEntity.getName());
        assertEquals(testAuthor.getBio(), savedAuthorEntity.getBio());
    }

    @Test
    void test_getPaginatedAuthors() {
        PageRequest pageable = PageRequest.of(0, 10);
        AuthorEntity authorEntity = new AuthorEntity(UUID.randomUUID(), "Author Name", "Bio", new HashSet<>());
        Page<AuthorEntity> authorEntities = new PageImpl<>(List.of(authorEntity), pageable, 1);

        when(authorRepository.findAllAuthorsWithBooks(pageable)).thenReturn(authorEntities);

        Page<Author> result = authorDaoAdapter.getPaginatedAuthors(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Author Name", result.getContent().getFirst().getName());
    }

    @Test
    void test_searchAuthors() {
        String query = "Author";
        PageRequest pageable = PageRequest.of(0, 10);
        AuthorEntity authorEntity = new AuthorEntity(UUID.randomUUID(), "Author Name", "Bio", new HashSet<>());
        Page<AuthorEntity> authorEntities = new PageImpl<>(List.of(authorEntity), pageable, 1);

        when(authorRepository.searchAuthorsByQuery(any(), any())).thenReturn(authorEntities);

        Page<Author> result = authorDaoAdapter.searchAuthors(query, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Author Name", result.getContent().getFirst().getName());
    }

    @Test
    void test_updateAuthor() {
        UUID authorId = UUID.randomUUID();
        Author newAuthor = new Author(authorId, "Updated Name", "Updated Bio", new HashSet<>());
        AuthorEntity existingAuthorEntity = new AuthorEntity(authorId, "Old Name", "Old Bio", new HashSet<>());

        when(authorRepository.findById(authorId)).thenReturn(Optional.of(existingAuthorEntity));

        authorDaoAdapter.updateAuthor(authorId, newAuthor);

        assertEquals("Updated Name", existingAuthorEntity.getName());
        assertEquals("Updated Bio", existingAuthorEntity.getBio());
        verify(authorRepository).save(existingAuthorEntity);
    }

    @Test
    void test_updateAuthor_throwsException() {
        UUID authorId = UUID.randomUUID();
        Author newAuthor = new Author(authorId, "Updated Name", "Updated Bio", new HashSet<>());

        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThrows(AuthorNotFoundException.class, () -> authorDaoAdapter.updateAuthor(authorId, newAuthor));
    }

    @Test
    void test_deleteAuthor() {
        UUID authorId = UUID.randomUUID();

        authorDaoAdapter.deleteAuthor(authorId);

        verify(authorRepository).deleteById(authorId);
    }

    @Test
    void test_searchAuthorByName() {
        String authorName = "Author Name";
        Set<BookEntity> bookEntities = new HashSet<>();
        bookEntities.add(BookEntity.builder()
                .bookId(UUID.randomUUID()).title("Book Title 1").isbn("1234567890")
                .publicationYear(2022).availability(true).createdAt(LocalDate.now())
                .authors(new HashSet<>()).transactions(new ArrayList<>()).build());
        bookEntities.add(BookEntity.builder()
                .bookId(UUID.randomUUID()).title("Book Title 2").isbn("0987654321")
                .publicationYear(2023).availability(false).createdAt(LocalDate.now())
                .authors(new HashSet<>()).transactions(new ArrayList<>()).build());

        AuthorEntity authorEntity = new AuthorEntity(
                UUID.randomUUID(),
                authorName,
                "Bio",
                bookEntities
        );

        when(authorRepository.findByName(authorName)).thenReturn(Optional.of(authorEntity));

        Optional<Author> result = authorDaoAdapter.searchAuthorByName(authorName);

        assertTrue(result.isPresent());
        assertEquals(authorName, result.get().getName());
    }

    @Test
    void test_searchAuthorByName_notFound() {
        String authorName = "Nonexistent Author";
        when(authorRepository.findByName(authorName)).thenReturn(Optional.empty());

        Optional<Author> result = authorDaoAdapter.searchAuthorByName(authorName);

        assertFalse(result.isPresent());
    }

    @Test
    void test_searchAuthorByID() {
        UUID authorId = UUID.randomUUID();
        AuthorEntity authorEntity = new AuthorEntity(authorId, "Author Name", "Bio", new HashSet<>());
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(authorEntity));

        Optional<Author> result = authorDaoAdapter.searchAuthorByID(authorId);

        assertTrue(result.isPresent());
        assertEquals(authorId, result.get().getAuthorId());
    }

    @Test
    void test_searchAuthorByID_notFound() {
        UUID authorId = UUID.randomUUID();
        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        Optional<Author> result = authorDaoAdapter.searchAuthorByID(authorId);

        assertFalse(result.isPresent());
    }
}
