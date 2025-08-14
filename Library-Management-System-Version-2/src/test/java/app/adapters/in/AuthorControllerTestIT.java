package app.adapters.in;

import app.adapters.in.dto.CreateNewAuthor;
import app.adapters.out.H2.repositories.AuthorRepository;
import app.domain.models.Author;
import app.domain.services.AuthorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "user")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("integration")
class AuthorControllerTestIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthorService authorService;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Test
    public void testCreateNewAuthor() throws Exception{
        CreateNewAuthor newAuthor = new CreateNewAuthor("Test Author", "test");

        mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAuthor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Author"))
                .andExpect(jsonPath("$.bio").value("test"));
    }
    @Test
    public void testGetAuthorByID() throws Exception{
        Author author = authorService.createNewAuthor(
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
        Author author = authorService.createNewAuthor(
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
        Author author = authorService.createNewAuthor(
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
        // First page, size 3 (Normal case)
        mockMvc.perform(get("/authors/paginated")
                        .param("page", "0")
                        .param("size", "3")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/authors/paginated?page=0&size=3")))
                .andExpect(header().string("next", Matchers.containsString("/authors/paginated?page=1&size=3")))
                .andExpect(header().doesNotExist("prev"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3)) // 3 authors expected
                .andExpect(jsonPath("$.data[0].name").value("Dante Alighieri"))
                .andExpect(jsonPath("$.data[1].name").value("F. Scott Fitzgerald"))
                .andExpect(jsonPath("$.data[2].name").value("George Orwell"));

        // Second page, size 3 (Normal case)
        mockMvc.perform(get("/authors/paginated")
                        .param("page", "1")
                        .param("size", "3")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/authors/paginated?page=1&size=3")))
                .andExpect(header().string("prev", Matchers.containsString("/authors/paginated?page=0&size=3")))
                .andExpect(header().string("next", Matchers.containsString("/authors/paginated?page=2&size=3")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3)) // 3 authors expected
                .andExpect(jsonPath("$.data[0].name").value("Harper Lee"))
                .andExpect(jsonPath("$.data[1].name").value("Herman Melville"))
                .andExpect(jsonPath("$.data[2].name").value("Homer"));

        // Third page, fewer items (size adjusted dynamically)
        mockMvc.perform(get("/authors/paginated")
                        .param("page", "2")
                        .param("size", "3")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(header().string("self", Matchers.containsString("/authors/paginated?page=2&size=3")))
                .andExpect(header().string("prev", Matchers.containsString("/authors/paginated?page=1&size=3")))
                .andExpect(header().string("next", Matchers.containsString("/authors/paginated?page=3&size=3"))) // Adjusted size
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3)) // 3 authors expected
                .andExpect(jsonPath("$.data[0].name").value("J.D. Salinger"))
                .andExpect(jsonPath("$.data[1].name").value("J.R.R. Tolkien"))
                .andExpect(jsonPath("$.data[2].name").value("Jane Austen"));

        // Last page with 1 item (dynamically adjusted size)
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
    @Test
    public void testUpdateAuthor() throws Exception {
        Author author = authorService.createNewAuthor(
                new CreateNewAuthor("Test Author", "test"));

        Author authorToUpdate = new Author("Updated Author", "updated");
        mockMvc.perform(put("/authors/" + author.getAuthorId())  // Use the saved book's ID
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorToUpdate)))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Author updated successfully!"));

        assertEquals("Updated Author", authorRepository.findById(author.getAuthorId()).get().getName());
        assertEquals("updated", authorRepository.findById(author.getAuthorId()).get().getBio());
    }
    @AfterEach
    public void tearDown() {
        authorRepository.deleteAll();
    }
}