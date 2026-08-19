package app.adapters.output.catalog;

import app.domain.dto.CreateNewBook;
import app.domain.model.CatalogPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Exercises the shapes Open Library actually returns: a description that is sometimes a string
 * and sometimes an object, a free-text publish date, and editions whose authors only appear
 * under the other jscmd.
 */
@Tag("unit")
class OpenLibraryAdapterTest {

    private static final String BASE = "https://openlibrary.test";

    private MockRestServiceServer server;
    private OpenLibraryAdapter adapter;

    private void withResponses(String... bodies) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        for (String body : bodies) {
            server.expect(requestTo(org.hamcrest.Matchers.containsString("/api/books")))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        }
        adapter = new OpenLibraryAdapter(builder.build());
    }

    @Test
    void mapsTitleAuthorsYearAndStringDescription() {
        withResponses("""
                {"ISBN:9780061120084": {"details": {
                   "title": "To Kill a Mockingbird",
                   "authors": [{"name": "Harper Lee"}],
                   "publish_date": "2006",
                   "description": "One of the best-loved stories of all time."
                }}}
                """);

        Optional<CreateNewBook> found = adapter.findByIsbn("978-0-06-112008-4");

        assertThat(found).isPresent();
        CreateNewBook book = found.get();
        assertThat(book.getTitle()).isEqualTo("To Kill a Mockingbird");
        assertThat(book.getIsbn()).isEqualTo("9780061120084");
        assertThat(book.getPublicationYear()).isEqualTo(2006);
        assertThat(book.getDescription()).isEqualTo("One of the best-loved stories of all time.");
        assertThat(book.getAuthors()).singleElement()
                .satisfies(author -> assertThat(author.getName()).isEqualTo("Harper Lee"));
    }

    @Test
    void readsDescriptionGivenAsAnObject() {
        withResponses("""
                {"ISBN:9780451524935": {"details": {
                   "title": "Nineteen Eighty-Four",
                   "authors": [{"name": "George Orwell"}],
                   "publish_date": "1993?",
                   "description": {"type": "/type/text", "value": "A nightmarish vision."}
                }}}
                """);

        CreateNewBook book = adapter.findByIsbn("9780451524935").orElseThrow();

        assertThat(book.getDescription()).isEqualTo("A nightmarish vision.");
        // "1993?" is free text; only the four-digit run is a year.
        assertThat(book.getPublicationYear()).isEqualTo(1993);
    }

    @Test
    void fallsBackToTheOtherCommandWhenAuthorsAreMissing() {
        withResponses("""
                {"ISBN:9780451524935": {"details": {"title": "Nineteen Eighty-Four", "authors": []}}}
                """,
                """
                {"ISBN:9780451524935": {"authors": [{"name": "George Orwell"}]}}
                """);

        CreateNewBook book = adapter.findByIsbn("9780451524935").orElseThrow();

        assertThat(book.getAuthors()).singleElement()
                .satisfies(author -> assertThat(author.getName()).isEqualTo("George Orwell"));
    }

    @Test
    void returnsEmptyForAnUnknownIsbn() {
        withResponses("{}");

        assertThat(adapter.findByIsbn("0000000000000")).isEmpty();
    }

    @Test
    void returnsEmptyWithoutAnIsbn() {
        withResponses();

        assertThat(adapter.findByIsbn("   ")).isEmpty();
        assertThat(adapter.findByIsbn(null)).isEmpty();
    }

    /** A catalogue outage must not stop a librarian adding the book by hand. */
    @Test
    void returnsEmptyWhenTheCatalogueIsDown() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo(org.hamcrest.Matchers.containsString("/api/books")))
                .andRespond(withServerError());

        assertThat(new OpenLibraryAdapter(builder.build()).findByIsbn("9780061120084")).isEmpty();
    }

    private void withSearchResponse(String body) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/search.json")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        adapter = new OpenLibraryAdapter(builder.build());
    }

    @Test
    void searchReportsTheWholeResultCountNotJustThisPage() {
        withSearchResponse("""
                {"numFound": 1174, "docs": [
                   {"title": "Dune", "author_name": ["Frank Herbert"],
                    "first_publish_year": 1965, "isbn": ["0441013597", "9780441013593"], "cover_i": 1234}
                ]}
                """);

        CatalogPage page = adapter.search("dune", 0, 20);

        // The picker needs the catalogue's total to offer page 47, not the size of this page.
        assertThat(page.totalItems()).isEqualTo(1174);
        assertThat(page.results()).singleElement().satisfies(candidate -> {
            assertThat(candidate.title()).isEqualTo("Dune");
            assertThat(candidate.publicationYear()).isEqualTo(1965);
            assertThat(candidate.authors()).containsExactly("Frank Herbert");
            assertThat(candidate.coverId()).isEqualTo(1234L);
            // The 13-digit ISBN is preferred over the 10-digit one.
            assertThat(candidate.isbn()).isEqualTo("9780441013593");
        });
    }

    /** Nothing can be imported against a hit with no ISBN, so it is dropped rather than shown. */
    @Test
    void searchSkipsResultsWithoutAnIsbn() {
        withSearchResponse("""
                {"numFound": 2, "docs": [
                   {"title": "No ISBN Here", "author_name": ["Anon"]},
                   {"title": "Dune", "isbn": ["9780441013593"]}
                ]}
                """);

        assertThat(adapter.search("dune", 0, 20).results())
                .singleElement()
                .satisfies(candidate -> assertThat(candidate.title()).isEqualTo("Dune"));
    }

    @Test
    void searchAsksOpenLibraryForTheRightPage() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        // Callers count pages from zero; Open Library counts from one.
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("page=3")))
                .andRespond(withSuccess("{\"numFound\": 0, \"docs\": []}", MediaType.APPLICATION_JSON));

        new OpenLibraryAdapter(builder.build()).search("dune", 2, 20);

        mockServer.verify();
    }

    @Test
    void searchReturnsEmptyForABlankQuery() {
        withResponses();

        assertThat(adapter.search("  ", 0, 20).results()).isEmpty();
        assertThat(adapter.search(null, 0, 20).totalItems()).isZero();
    }

    @Test
    void searchReturnsEmptyWhenTheCatalogueIsDown() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo(org.hamcrest.Matchers.containsString("/search.json")))
                .andRespond(withServerError());

        assertThat(new OpenLibraryAdapter(builder.build()).search("dune", 0, 20).results()).isEmpty();
    }
}
