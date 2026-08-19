package app.adapters.input;

import app.adapters.output.repositories.AuthorRepository;
import app.adapters.output.repositories.BookRepository;
import app.domain.dto.CreateNewAuthor;
import app.domain.dto.CreateNewBook;
import app.domain.model.CatalogCandidate;
import app.domain.model.CatalogPage;
import app.domain.port.input.BookUseCase;
import app.domain.port.output.BookCatalogPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Discovering books the library does not hold yet. The external catalogue is stubbed: these tests
 * are about what the endpoint does with an answer, not about Open Library being up.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "member")
@Tag("integration")
class BookDiscoveryControllerTestIT {

    private static final String DUNE_ISBN = "9780441013593";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BookUseCase bookUseCase;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;

    @MockitoBean
    private BookCatalogPort bookCatalogPort;

    private CatalogCandidate dune() {
        return new CatalogCandidate("Dune", DUNE_ISBN, 1965, List.of("Frank Herbert"), 1234L);
    }

    @Test
    void discoverReportsTheCataloguesTotalSoThePickerCanPage() throws Exception {
        when(bookCatalogPort.search(anyString(), anyInt(), anyInt()))
                .thenReturn(new CatalogPage(List.of(dune()), 1174));

        mockMvc.perform(get("/books/discover").param("query", "dune").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1174))
                .andExpect(jsonPath("$.totalPages").value(59))
                .andExpect(jsonPath("$.data[0].title").value("Dune"))
                .andExpect(jsonPath("$.data[0].isbn").value(DUNE_ISBN))
                .andExpect(jsonPath("$.data[0].stocked").value(false));
    }

    /** Marking stocked hits up front is what turns "Add to library" into "Already on the shelves". */
    @Test
    void discoverMarksBooksTheLibraryAlreadyHolds() throws Exception {
        bookUseCase.createNewBook(new CreateNewBook("Dune", DUNE_ISBN, 1965,
                List.of(new CreateNewAuthor("Frank Herbert", ""))));
        when(bookCatalogPort.search(anyString(), anyInt(), anyInt()))
                .thenReturn(new CatalogPage(List.of(dune()), 1));

        mockMvc.perform(get("/books/discover").param("query", "dune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stocked").value(true));
    }

    private String addRequest(String title, String isbn, int year, String author) {
        return """
                {"title": "%s", "isbn": "%s", "publicationYear": %d, "authors": ["%s"]}
                """.formatted(title, isbn, year, author);
    }

    @Test
    void aMemberCanPutABookOnTheShelves() throws Exception {
        mockMvc.perform(post("/books/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest("Dune", DUNE_ISBN, 1965, "Frank Herbert")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("\"Dune\" is now on the shelves."))
                .andExpect(jsonPath("$.bookId").exists());

        mockMvc.perform(get("/books").param("isbn", DUNE_ISBN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Dune"))
                .andExpect(jsonPath("$[0].publicationYear").value(1965))
                .andExpect(jsonPath("$[0].authors[0].name").value("Frank Herbert"));
    }

    /**
     * The shelf gets the book that was on the card. A search hit's ISBN belongs to one particular
     * edition, so re-deriving the book from it server-side once turned a click on "A Wizard of
     * Earthsea" into the Polish edition, "Czarnoksiężnik z Archipelagu".
     */
    @Test
    void theBookStockedIsTheOneTheReaderSaw() throws Exception {
        when(bookCatalogPort.findByIsbn(anyString())).thenReturn(Optional.of(new CreateNewBook(
                "Czarnoksiężnik z Archipelagu", DUNE_ISBN, 2010, null,
                List.of(new CreateNewAuthor("Ursula K. Le Guin", "")))));

        mockMvc.perform(post("/books/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest("A Wizard of Earthsea", DUNE_ISBN, 1968, "Ursula K. Le Guin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("\"A Wizard of Earthsea\" is now on the shelves."));

        mockMvc.perform(get("/books").param("isbn", DUNE_ISBN))
                .andExpect(jsonPath("$[0].title").value("A Wizard of Earthsea"))
                .andExpect(jsonPath("$[0].publicationYear").value(1968));
    }

    /** A book with no author in the catalogue still has to satisfy "at least one author". */
    @Test
    void aBookWithNoListedAuthorIsStockedAsUnknown() throws Exception {
        mockMvc.perform(post("/books/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Dune\", \"isbn\": \"" + DUNE_ISBN
                                + "\", \"publicationYear\": 1965, \"authors\": []}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/books").param("isbn", DUNE_ISBN))
                .andExpect(jsonPath("$[0].authors[0].name").value("Unknown author"));
    }

    @Test
    void addingABookTheLibraryAlreadyHoldsIsAConflictNotAnError() throws Exception {
        bookUseCase.createNewBook(new CreateNewBook("Dune", DUNE_ISBN, 1965,
                List.of(new CreateNewAuthor("Frank Herbert", ""))));

        mockMvc.perform(post("/books/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest("Dune", DUNE_ISBN, 1965, "Frank Herbert")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This book is already on the shelves."));
    }

    @Test
    void addingWithoutATitleIsRejected() throws Exception {
        mockMvc.perform(post("/books/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest("", DUNE_ISBN, 1965, "Frank Herbert")))
                .andExpect(status().isBadRequest());
    }

    @AfterEach
    void tearDown() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }
}
