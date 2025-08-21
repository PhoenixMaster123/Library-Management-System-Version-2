package app.domain.services.unitTests;

import app.domain.dto.CreateNewAuthor;
import app.domain.model.Author;
import app.domain.port.output.AuthorRepositoryPort;
import app.domain.port.input.AuthorUseCase;
import app.domain.services.AuthorService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("unit")
class AuthorUseCaseTest {

    @Mock
    private AuthorRepositoryPort mockedAuthorRepositoryPort;


    @BeforeAll
    void setup() {
        mockedAuthorRepositoryPort = mock(AuthorRepositoryPort.class);
    }
        @Test
        void createNewAuthor_Success() {
            AuthorUseCase authorUseCase = new AuthorService(mockedAuthorRepositoryPort);
            CreateNewAuthor newAuthor = new CreateNewAuthor("John Doe", "Author bio");

            when(mockedAuthorRepositoryPort.searchAuthorByName("John Doe")).thenReturn(Optional.empty());
            doAnswer(invocation -> {
                Author savedAuthor = invocation.getArgument(0);
                when(mockedAuthorRepositoryPort.searchAuthorByName(savedAuthor.getName())).thenReturn(Optional.of(savedAuthor));
                return null;
            }).when(mockedAuthorRepositoryPort).saveAuthor(any(Author.class));

            authorUseCase.createNewAuthor(newAuthor);

            ArgumentCaptor<Author> captor = ArgumentCaptor.forClass(Author.class);
            verify(mockedAuthorRepositoryPort).saveAuthor(captor.capture());
            assertEquals("John Doe", captor.getValue().getName());
            assertEquals("Author bio", captor.getValue().getBio());
        }
        @Test
        void createNewAuthor_ThrowsException_WhenNameExists() {
            AuthorUseCase authorUseCase = new AuthorService(mockedAuthorRepositoryPort);
            CreateNewAuthor newAuthor = new CreateNewAuthor("John Doe", "Author bio");

            when(mockedAuthorRepositoryPort.searchAuthorByName("John Doe")).thenReturn(Optional.of(new Author()));

            assertThrows(IllegalArgumentException.class, () -> authorUseCase.createNewAuthor(newAuthor));
        }
        @Test
        void searchAuthorByName_Found() {
            AuthorUseCase authorUseCase = new AuthorService(mockedAuthorRepositoryPort);
            String name = "John Doe";
            Author author = new Author(name, "Author bio");

            when(mockedAuthorRepositoryPort.searchAuthorByName(name)).thenReturn(Optional.of(author));

            Optional<Author> result = authorUseCase.getAuthorByName(name);

            assertTrue(result.isPresent());
            assertEquals(name, result.get().getName());
            verify(mockedAuthorRepositoryPort, times(1)).searchAuthorByName(name);
        }

        @Test
        void searchAuthorById_Found() {
            AuthorUseCase authorUseCase = new AuthorService(mockedAuthorRepositoryPort);
            UUID authorId = UUID.randomUUID();
            Author author = new Author("John Doe", "Author bio");
            author.setAuthorId(authorId);

            when(mockedAuthorRepositoryPort.searchAuthorByID(authorId)).thenReturn(Optional.of(author));

            Optional<Author> result = authorUseCase.findAuthorById(authorId);

            assertTrue(result.isPresent());
            assertEquals("John Doe", result.get().getName());
            verify(mockedAuthorRepositoryPort, times(1)).searchAuthorByID(authorId);
        }
        @Test
        void updateAuthor_Success() {
            AuthorUseCase authorUseCase = new AuthorService(mockedAuthorRepositoryPort);
            UUID authorId = UUID.randomUUID();
            Author existingAuthor = new Author(authorId, "John Doe", "example");
            Author updatedAuthor = new Author("John Update", "updated bio");

            when(mockedAuthorRepositoryPort.searchAuthorByID(authorId)).thenReturn(Optional.of(existingAuthor));

            authorUseCase.updateAuthor(authorId, updatedAuthor);

            verify(mockedAuthorRepositoryPort).updateAuthor(eq(authorId), argThat(updated -> {
                assertEquals(updatedAuthor.getName(), updated.getName());
                assertEquals(updatedAuthor.getBio(), updated.getBio());
                return true;
            }));
        }
        @Test
        public void testGetPaginatedAuthors() {
            AuthorUseCase authorUseCase = new AuthorService(mockedAuthorRepositoryPort);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Author> mockPage = new PageImpl<>(List.of(new Author(UUID.randomUUID(),
                    "John Doe", "example"),
                    new Author(UUID.randomUUID(),"Jane Smith", "example")));
            Mockito.when(mockedAuthorRepositoryPort.getPaginatedAuthors(pageable)).thenReturn(mockPage);

            Page<Author> result = authorUseCase.getPaginatedAuthors(pageable);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            Mockito.verify(mockedAuthorRepositoryPort, Mockito.times(1)).getPaginatedAuthors(pageable);
        }

        @Test
        public void testSearchAuthors() {
            AuthorUseCase authorUseCase = new AuthorService(mockedAuthorRepositoryPort);
            String query = "John";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Author> mockPage = new PageImpl<>(List.of(new Author("John Doe", "example")));
            Mockito.when(mockedAuthorRepositoryPort.searchAuthors(query, pageable)).thenReturn(mockPage);


            Page<Author> result = authorUseCase.searchAuthors(query, pageable);


            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("John Doe", result.getContent().getFirst().getName());
            Mockito.verify(mockedAuthorRepositoryPort, Mockito.times(1)).searchAuthors(query, pageable);
        }
    }

