package app.adapters.output.catalog;

import app.domain.dto.CreateNewAuthor;
import app.domain.dto.CreateNewBook;
import app.domain.model.CatalogCandidate;
import app.domain.model.CatalogPage;
import app.domain.port.output.BookCatalogPort;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Looks books up in Open Library, reading raw JSON because the shape varies by edition. */
@Component
@Slf4j
public class OpenLibraryAdapter implements BookCatalogPort {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");
    private static final int MAX_DESCRIPTION = 4000;

    /** Open Library refuses larger pages, and a reader has no use for one anyway. */
    private static final int MAX_PAGE_SIZE = 100;

    private final RestClient restClient;

    /** Takes the shared catalogue client, so tests can pass a stubbed one. */
    public OpenLibraryAdapter(RestClient catalogRestClient) {
        this.restClient = catalogRestClient;
    }

    /** The book for one ISBN, or empty. Only hits are cached, so a timeout is retried. */
    @Override
    @Cacheable(cacheNames = "catalogLookup", key = "#isbn", unless = "#result == null")
    public Optional<CreateNewBook> findByIsbn(String isbn) {
        String cleaned = isbn == null ? "" : isbn.replaceAll("[^0-9Xx]", "");
        if (cleaned.isBlank()) {
            return Optional.empty();
        }

        JsonNode details = fetch(cleaned, "details");
        if (details == null || details.isMissingNode()) {
            return Optional.empty();
        }
        JsonNode record = details.path("details");

        List<CreateNewAuthor> authors = readAuthors(record);
        if (authors.isEmpty()) {
            // Some editions carry no authors under `details` but do under `data`.
            JsonNode data = fetch(cleaned, "data");
            if (data != null) {
                authors = readAuthors(data);
            }
        }

        return Optional.of(new CreateNewBook(
                record.path("title").asText(null),
                cleaned,
                parseYear(record.path("publish_date").asText(null)),
                readDescription(record),
                authors));
    }

    /** One page of search hits. Cached because readers page back and forth over the same query. */
    @Override
    @Cacheable(cacheNames = "catalogSearch",
            key = "#query.toLowerCase() + '#' + #page + '#' + #size",
            unless = "#result.results().isEmpty()")
    public CatalogPage search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return CatalogPage.empty();
        }

        int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        // Open Library counts pages from one; every caller here counts from zero.
        int pageNumber = Math.max(page, 0) + 1;

        try {
            JsonNode body = restClient.get()
                    .uri(uri -> uri.path("/search.json")
                            .queryParam("q", query)
                            .queryParam("page", pageNumber)
                            .queryParam("limit", pageSize)
                            .queryParam("fields", "title,author_name,first_publish_year,isbn,cover_i")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (body == null) {
                return CatalogPage.empty();
            }

            List<CatalogCandidate> results = new ArrayList<>();
            for (JsonNode doc : body.path("docs")) {
                // Without an ISBN there is nothing to import against, so skip the entry.
                String isbn = firstIsbn(doc);
                if (isbn == null) {
                    continue;
                }
                JsonNode cover = doc.path("cover_i");
                results.add(new CatalogCandidate(
                        doc.path("title").asText(null),
                        isbn,
                        doc.path("first_publish_year").asInt(0),
                        readAuthorNames(doc),
                        cover.isNumber() ? cover.asLong() : null));
            }

            // numFound counts every edition matching the query, including the ISBN-less ones
            // skipped above, so the last page can come back shorter than it promises.
            return new CatalogPage(results, body.path("numFound").asInt(results.size()));
        } catch (Exception e) {
            log.warn("Open Library search for '{}' failed: {}", query, e.getMessage());
            return CatalogPage.empty();
        }
    }

    /** Search results list every edition's ISBN; the first 13-digit one is the most useful. */
    private String firstIsbn(JsonNode doc) {
        String fallback = null;
        for (JsonNode isbn : doc.path("isbn")) {
            String value = isbn.asText();
            if (value.length() == 13) {
                return value;
            }
            if (fallback == null) {
                fallback = value;
            }
        }
        return fallback;
    }

    /** The search endpoint gives author names as plain strings, not objects. */
    private List<String> readAuthorNames(JsonNode doc) {
        List<String> authors = new ArrayList<>();
        for (JsonNode name : doc.path("author_name")) {
            authors.add(name.asText());
        }
        return authors;
    }

    /** Returns null on any failure: a lookup is a convenience, never a precondition. */
    private JsonNode fetch(String isbn, String jscmd) {
        try {
            JsonNode body = restClient.get()
                    .uri(uri -> uri.path("/api/books")
                            .queryParam("bibkeys", "ISBN:" + isbn)
                            .queryParam("format", "json")
                            .queryParam("jscmd", jscmd)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            return body == null ? null : body.path("ISBN:" + isbn);
        } catch (Exception e) {
            log.warn("Open Library lookup for ISBN {} ({}) failed: {}", isbn, jscmd, e.getMessage());
            return null;
        }
    }

    /** Author records from a lookup, where they are objects. */
    private List<CreateNewAuthor> readAuthors(JsonNode node) {
        List<CreateNewAuthor> authors = new ArrayList<>();
        for (JsonNode author : node.path("authors")) {
            String name = author.path("name").asText(null);
            if (name != null && !name.isBlank()) {
                authors.add(new CreateNewAuthor(name, ""));
            }
        }
        return authors;
    }

    /** {@code description} is sometimes a string and sometimes {@code {"value": "..."}}. */
    private String readDescription(JsonNode record) {
        JsonNode description = record.path("description");
        String text = description.isObject() ? description.path("value").asText(null) : description.asText(null);

        if (text == null || text.isBlank()) {
            return null;
        }
        return text.length() > MAX_DESCRIPTION ? text.substring(0, MAX_DESCRIPTION) : text;
    }

    /** publish_date is free text: "1993?", "2006", "June 1993". Take the first four-digit run. */
    private int parseYear(String publishDate) {
        if (publishDate == null) {
            return 0;
        }
        Matcher matcher = YEAR.matcher(publishDate);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }
}
