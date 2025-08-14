package app.domain.services.integrationTests;

import app.adapters.in.dto.CreateNewAuthor;
import app.adapters.out.H2.repositories.AuthorRepository;
import app.domain.models.Author;
import app.domain.port.AuthorDao;
import app.domain.services.AuthorService;
import jakarta.transaction.Transactional;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
public class AuthorServiceIT {
    @Autowired
    private AuthorDao realAuthorDao;

    @Autowired
    private AuthorService realAuthorService;

    @Autowired
    private AuthorRepository authorRepository;
        @BeforeEach
        void setup() {
            authorRepository.deleteAll();
        }
        @Test
        void createNewAuthor_IntegrationTest() {
            CreateNewAuthor newAuthor = new CreateNewAuthor("Jane Smith", "Author bio");

            Author createdAuthor = realAuthorService.createNewAuthor(newAuthor);

            assertNotNull(createdAuthor.getAuthorId());
            assertEquals("Jane Smith", createdAuthor.getName());
            assertTrue(realAuthorDao.searchAuthorByName("Jane Smith").isPresent());
        }

        @Test
        void createNewAuthor_AuthorWithTheSameNameAlreadyExists_IntegrationTest() {
            CreateNewAuthor newAuthor = new CreateNewAuthor("Jane Smith", "Author bio");
            realAuthorService.createNewAuthor(newAuthor);
            assertThrows(IllegalArgumentException.class, () -> realAuthorService.createNewAuthor(newAuthor));
        }

        @Test
        void searchAuthorByName_IntegrationTest() {
            String name = "Unique Author";
            Author author = new Author(name, "Author bio");
            realAuthorDao.addAuthor(author);

            Optional<Author> result = realAuthorService.getAuthorByName(name);

            assertTrue(result.isPresent());
            assertEquals(name, result.get().getName());
        }

        @Test
        void searchAuthorById_IntegrationTest() {

            Author author = new Author("Author Name", "Author bio");


            realAuthorDao.addAuthor(author);


            Optional<Author> persistedAuthor = realAuthorDao.searchAuthorByName("Author Name");

            assertTrue(persistedAuthor.isPresent());


            Optional<Author> result = realAuthorService.findAuthorById(persistedAuthor.get().getAuthorId());


            assertTrue(result.isPresent());
            assertEquals("Author Name", result.get().getName());
            assertEquals("Author bio", result.get().getBio());
            assertEquals(persistedAuthor.get().getAuthorId(), result.get().getAuthorId());
        }

        @Test
        void testGetPaginatedAuthors_Success() {

            long baseCount = authorRepository.count();

            Author author1 = new Author(null, "Italo Calvino", "English");
            Author author2 = new Author(null, "Lev Tolstoy", "English");
            Author author3 = new Author(null, "Johann Wolfgang von Goethe", "English");

            realAuthorDao.addAuthor(author1);
            realAuthorDao.addAuthor(author2);
            realAuthorDao.addAuthor(author3);

            Pageable pageable = PageRequest.of(0, 2);


            Page<Author> authorsPage = realAuthorService.getPaginatedAuthors(pageable);


            assertThat(authorsPage.getContent()).hasSize(2);
            AssertionsForClassTypes.assertThat(authorsPage.getTotalElements()).isEqualTo(baseCount + 3);
        }

        @Test
        void testSearchAuthors_Success() {

            Author author1 = new Author(null, "Italo Calvino", "English");
            Author author2 = new Author(null, "Lev Tolstoy", "English");
            Author author3 = new Author(null, "Johann Wolfgang von Goethe", "English");

            realAuthorDao.addAuthor(author1);
            realAuthorDao.addAuthor(author2);
            realAuthorDao.addAuthor(author3);

            Pageable pageable = PageRequest.of(0, 10);


            Page<Author> searchedAuthors = realAuthorService.searchAuthors("Calvino", pageable);


            assertThat(searchedAuthors.getContent()).hasSize(1);
            AssertionsForClassTypes.assertThat(searchedAuthors.getContent().getFirst().getName()).isEqualTo("Italo Calvino");
        }

        @Test
        void testSearchAuthors_NoResults() {
            Author author1 = new Author(null, "Italo Calvino", "English");
            Author author2 = new Author(null, "Lev Tolstoy", "English");
            Author author3 = new Author(null, "Johann Wolfgang von Goethe", "English");

            realAuthorDao.addAuthor(author1);
            realAuthorDao.addAuthor(author2);
            realAuthorDao.addAuthor(author3);

            Pageable pageable = PageRequest.of(0, 10);

            Page<Author> searchedAuthors = realAuthorService.searchAuthors("NonexistentAuthor", pageable);

            assertThat(searchedAuthors.getContent()).isEmpty();
        }
        @AfterEach
        void tearDown() {
            authorRepository.deleteAll();
        }
    }
