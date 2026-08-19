package app.adapters.input;

import app.domain.dto.CreateNewAuthor;
import app.adapters.output.repositories.AuthorRepository;
import app.domain.model.Author;
import app.domain.port.input.AuthorUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user")
@Tag("integration")
class AuthorControllerTestIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthorUseCase authorUseCase;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAuthorByID() throws Exception{
        Author author = authorUseCase.createNewAuthor(
                new CreateNewAuthor("Test Author", "test"));

        mockMvc.perform(get("/authors/search")
                        .param("id", author.getAuthorId().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/authors/search")
                        .param("id", author.getAuthorId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Author"))
                .andExpect(jsonPath("$.data.bio").value("test"));

    }
    @Test
    public void getAuthorById_ShouldReturnAuthor_WhenAuthorExists() throws Exception {
        Author author = authorUseCase.createNewAuthor(
                new CreateNewAuthor("Test Author", "test"));
        mockMvc.perform(get("/authors/" + author.getAuthorId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Author retrieved successfully"))
                .andExpect(jsonPath("$.data.name").value("Test Author"));
    }

    @Test
    public void getAuthorById_ShouldReturnNotFound_WhenAuthorDoesNotExist() throws Exception {
        mockMvc.perform(get("/authors/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found"));
    }

    @Test
    public void getAuthorById_ShouldReturnBadRequest_WhenUUIDIsInvalid() throws Exception {
        mockMvc.perform(get("/authors/invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid UUID format"));
    }
    @Test
    public void testGetAuthorByName() throws Exception{
        Author author = authorUseCase.createNewAuthor(
                new CreateNewAuthor("Test Author", "test"));

        mockMvc.perform(get("/authors/search")
                        .param("name", author.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Author"))
                .andExpect(jsonPath("$.data.bio").value("test"));

    }
    @Test
    public void testGetBookByQuery_multipleResults() throws Exception {
        mockMvc.perform(get("/authors/search")
                        .param("query", "J")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/authors/search?query=J&page=0&size=3")))
                .andExpect(header().doesNotExist("next"))
                .andExpect(header().doesNotExist("prev"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("J.D. Salinger"))
                .andExpect(jsonPath("$.data[1].name").value("J.R.R. Tolkien"))
                .andExpect(jsonPath("$.data[2].name").value("Jane Austen"));
    }
    @Test
    public void testGetAuthorByName_NotFound() throws Exception {
        mockMvc.perform(get("/authors/search")
                        .param("name", "Nonexistent Author"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author with the given name not found"));
    }
    @Test
    public void testGetAuthorByID_NotFound() throws Exception {
        mockMvc.perform(get("/authors/search")
                        .param("id", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found"));
    }
    @Test
    public void testNoCriteriaProvided() throws Exception {
        mockMvc.perform(get("/authors/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No search criteria provided"));
    }
    @Test
    public void testGetAllAuthors() throws Exception {
        mockMvc.perform(get("/authors/paginated")
                        .param("page", "0")
                        .param("size", "3")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/authors/paginated?page=0&size=3")))
                .andExpect(header().string("next", Matchers.containsString("/authors/paginated?page=1&size=3")))
                .andExpect(header().doesNotExist("prev"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("Dante Alighieri"))
                .andExpect(jsonPath("$.data[1].name").value("F. Scott Fitzgerald"))
                .andExpect(jsonPath("$.data[2].name").value("George Orwell"));

        mockMvc.perform(get("/authors/paginated")
                        .param("page", "1")
                        .param("size", "3")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/authors/paginated?page=1&size=3")))
                .andExpect(header().string("prev", Matchers.containsString("/authors/paginated?page=0&size=3")))
                .andExpect(header().string("next", Matchers.containsString("/authors/paginated?page=2&size=3")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("Harper Lee"))
                .andExpect(jsonPath("$.data[1].name").value("Herman Melville"))
                .andExpect(jsonPath("$.data[2].name").value("Homer"));

        mockMvc.perform(get("/authors/paginated")
                        .param("page", "2")
                        .param("size", "3")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/authors/paginated?page=2&size=3")))
                .andExpect(header().string("prev", Matchers.containsString("/authors/paginated?page=1&size=3")))
                .andExpect(header().string("next", Matchers.containsString("/authors/paginated?page=3&size=3")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("J.D. Salinger"))
                .andExpect(jsonPath("$.data[1].name").value("J.R.R. Tolkien"))
                .andExpect(jsonPath("$.data[2].name").value("Jane Austen"));

        mockMvc.perform(get("/authors/paginated")
                        .param("page", "3")
                        .param("size", "3")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/authors/paginated?page=3&size=3")))
                .andExpect(header().string("prev", Matchers.containsString("/authors/paginated?page=2&size=3")))
                .andExpect(header().doesNotExist("next"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Leo Tolstoy"));
    }
    @AfterEach
    public void tearDown() {
        authorRepository.deleteAll();
    }
}