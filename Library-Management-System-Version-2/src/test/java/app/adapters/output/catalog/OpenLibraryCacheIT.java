package app.adapters.output.catalog;

import app.domain.dto.CreateNewBook;
import app.domain.model.CatalogPage;
import app.domain.port.output.BookCatalogPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The adapter as Spring actually wires it - behind the caching proxy - rather than constructed
 * by hand.
 *
 * <p>{@link OpenLibraryAdapterTest} builds the adapter directly, so it never evaluates the
 * {@code @Cacheable} expressions. That blind spot let a broken {@code unless} ship: Spring unwraps
 * an {@code Optional} result before evaluating it, so {@code #result.isEmpty()} threw EL1004 on
 * every successful lookup and turned adding a book into a 500.
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.cache.type=simple",
})
/*
 * Kept here after being removed from the controller ITs, and load-bearing for a different reason:
 * MockRestServiceServer lives on the RestClient bean, so its expectations accumulate for the life
 * of the context. These tests assert exact call counts - expect(once(), ...) then verify() - which
 * only holds with a server built fresh per test method. Three tests, so three contexts.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(OpenLibraryCacheIT.StubCatalogClient.class)
@Tag("integration")
class OpenLibraryCacheIT {

    private static final String ISBN = "9780061120084";

    /** Binds the adapter's client to a mock server instead of the real Open Library. */
    @TestConfiguration
    static class StubCatalogClient {
        static MockRestServiceServer server;

        @Bean
        RestClient catalogRestClient() {
            RestClient.Builder builder = RestClient.builder().baseUrl("https://openlibrary.test");
            server = MockRestServiceServer.bindTo(builder).build();
            return builder.build();
        }
    }

    @Autowired
    private BookCatalogPort bookCatalogPort;
    @Autowired
    private CacheManager cacheManager;

    @Test
    void aSuccessfulLookupIsCachedRatherThanThrowingOnTheUnlessExpression() {
        StubCatalogClient.server.expect(once(), requestTo(containsString("/api/books")))
                .andRespond(withSuccess("""
                        {"ISBN:9780061120084": {"details": {
                           "title": "To Kill a Mockingbird",
                           "authors": [{"name": "Harper Lee"}],
                           "publish_date": "2006"
                        }}}
                        """, MediaType.APPLICATION_JSON));

        Optional<CreateNewBook> first = bookCatalogPort.findByIsbn(ISBN);
        assertThat(first).isPresent();
        assertThat(first.get().getTitle()).isEqualTo("To Kill a Mockingbird");

        // Answered from the cache: the mock server was told to expect exactly one call, so a
        // second trip to Open Library would fail verify() below.
        assertThat(bookCatalogPort.findByIsbn(ISBN)).isPresent();
        StubCatalogClient.server.verify();

        assertThat(cacheManager.getCache("catalogLookup")).isNotNull();
        assertThat(cacheManager.getCache("catalogLookup").get(ISBN)).isNotNull();
    }

    /** A miss must not be remembered, or one catalogue outage hides the book for good. */
    @Test
    void anUnknownIsbnIsNotCached() {
        StubCatalogClient.server.expect(requestTo(containsString("/api/books")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(bookCatalogPort.findByIsbn("0000000000000")).isEmpty();
        assertThat(cacheManager.getCache("catalogLookup").get("0000000000000")).isNull();
    }

    @Test
    void aSearchPageIsCachedWithoutThrowing() {
        StubCatalogClient.server.expect(once(), requestTo(containsString("/search.json")))
                .andRespond(withSuccess("""
                        {"numFound": 42, "docs": [
                           {"title": "Dune", "isbn": ["9780441013593"], "author_name": ["Frank Herbert"]}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        CatalogPage first = bookCatalogPort.search("dune", 0, 20);
        assertThat(first.totalItems()).isEqualTo(42);

        assertThat(bookCatalogPort.search("dune", 0, 20).results()).hasSize(1);
        StubCatalogClient.server.verify();
    }
}
