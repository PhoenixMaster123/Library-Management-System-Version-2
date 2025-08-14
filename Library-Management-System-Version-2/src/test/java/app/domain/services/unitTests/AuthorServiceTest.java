package app.domain.services.unitTests;

import app.adapters.in.dto.CreateNewAuthor;
import app.domain.models.Author;
import app.domain.port.AuthorDao;
import app.domain.services.AuthorService;
import app.domain.services.impl.AuthorServiceImpl;
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
class AuthorServiceTest {

    @Mock
    private AuthorDao mockedAuthorDao;


    @BeforeAll
    void setup() {
        mockedAuthorDao = mock(AuthorDao.class);
    }
        @Test
        void createNewAuthor_Success() {
            AuthorService authorService = new AuthorServiceImpl(mockedAuthorDao);
            CreateNewAuthor newAuthor = new CreateNewAuthor("John Doe", "Author bio");

            when(mockedAuthorDao.searchAuthorByName("John Doe")).thenReturn(Optional.empty());
            doAnswer(invocation -> {
                Author savedAuthor = invocation.getArgument(0);
                when(mockedAuthorDao.searchAuthorByName(savedAuthor.getName())).thenReturn(Optional.of(savedAuthor));
                return null;
            }).when(mockedAuthorDao).addAuthor(any(Author.class));

            authorService.createNewAuthor(newAuthor);

            ArgumentCaptor<Author> captor = ArgumentCaptor.forClass(Author.class);
            verify(mockedAuthorDao).addAuthor(captor.capture());
            assertEquals("John Doe", captor.getValue().getName());
            assertEquals("Author bio", captor.getValue().getBio());
        }
        @Test
        void createNewAuthor_ThrowsException_WhenNameExists() {
            AuthorService authorService = new AuthorServiceImpl(mockedAuthorDao);
            CreateNewAuthor newAuthor = new CreateNewAuthor("John Doe", "Author bio");

            when(mockedAuthorDao.searchAuthorByName("John Doe")).thenReturn(Optional.of(new Author()));

            assertThrows(IllegalArgumentException.class, () -> authorService.createNewAuthor(newAuthor));
        }
        @Test
        void searchAuthorByName_Found() {
            AuthorService authorService = new AuthorServiceImpl(mockedAuthorDao);
            String name = "John Doe";
            Author author = new Author(name, "Author bio");

            when(mockedAuthorDao.searchAuthorByName(name)).thenReturn(Optional.of(author));

            Optional<Author> result = authorService.getAuthorByName(name);

            assertTrue(result.isPresent());
            assertEquals(name, result.get().getName());
            verify(mockedAuthorDao, times(1)).searchAuthorByName(name);
        }

        @Test
        void searchAuthorById_Found() {
            AuthorService authorService = new AuthorServiceImpl(mockedAuthorDao);
            UUID authorId = UUID.randomUUID();
            Author author = new Author("John Doe", "Author bio");
            author.setAuthorId(authorId);

            when(mockedAuthorDao.searchAuthorByID(authorId)).thenReturn(Optional.of(author));

            Optional<Author> result = authorService.findAuthorById(authorId);

            assertTrue(result.isPresent());
            assertEquals("John Doe", result.get().getName());
            verify(mockedAuthorDao, times(1)).searchAuthorByID(authorId);
        }
        @Test
        void updateAuthor_Success() {
            AuthorService authorService = new AuthorServiceImpl(mockedAuthorDao);
            UUID authorId = UUID.randomUUID();
            Author existingAuthor = new Author(authorId, "John Doe", "example");
            Author updatedAuthor = new Author("John Update", "updated bio");

            when(mockedAuthorDao.searchAuthorByID(authorId)).thenReturn(Optional.of(existingAuthor));

            authorService.updateAuthor(authorId, updatedAuthor);

            verify(mockedAuthorDao).updateAuthor(eq(authorId), argThat(updated -> {
                assertEquals(updatedAuthor.getName(), updated.getName());
                assertEquals(updatedAuthor.getBio(), updated.getBio());
                return true;
            }));
        }
        @Test
        public void testGetPaginatedAuthors() {
            AuthorService authorService = new AuthorServiceImpl(mockedAuthorDao);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Author> mockPage = new PageImpl<>(List.of(new Author(UUID.randomUUID(),
                    "John Doe", "example"),
                    new Author(UUID.randomUUID(),"Jane Smith", "example")));
            Mockito.when(mockedAuthorDao.getPaginatedAuthors(pageable)).thenReturn(mockPage);

            Page<Author> result = authorService.getPaginatedAuthors(pageable);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            Mockito.verify(mockedAuthorDao, Mockito.times(1)).getPaginatedAuthors(pageable);
        }

        @Test
        public void testSearchAuthors() {
            AuthorService authorService = new AuthorServiceImpl(mockedAuthorDao);
            String query = "John";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Author> mockPage = new PageImpl<>(List.of(new Author("John Doe", "example")));
            Mockito.when(mockedAuthorDao.searchAuthors(query, pageable)).thenReturn(mockPage);


            Page<Author> result = authorService.searchAuthors(query, pageable);


            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("John Doe", result.getContent().getFirst().getName());
            Mockito.verify(mockedAuthorDao, Mockito.times(1)).searchAuthors(query, pageable);
        }
    }

