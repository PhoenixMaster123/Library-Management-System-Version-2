package app.adapters.input.rest;

import app.domain.model.Author;
import app.domain.port.input.AuthorUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/** Read-only author browsing. Creating and editing authors lives in {@link AdminController}. */
@RestController
@RequestMapping("/authors")
@Tag(name = "Author Controller", description = "Browsing and searching authors")
@RequiredArgsConstructor
public class AuthorController extends PaginatedController {

    private final AuthorUseCase authorUseCase;

    @GetMapping(value = "/{id}",
            produces = {"application/single-author-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Get author by ID")
    public ResponseEntity<Map<String, Object>> getAuthorById(@PathVariable UUID id) {
        Optional<Author> authorOpt = authorUseCase.findAuthorById(id);

        if (authorOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Author not found", "authorId", id));
        }

        CacheControl cacheControl = CacheControl
                .maxAge(30, TimeUnit.SECONDS)
                .cachePrivate()
                .noTransform();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("Vary", "Accept")
                .body(Map.of("message", "Author retrieved successfully", "data", authorOpt.get()));
    }

    @GetMapping(value = "/search",
            produces = {"application/paginated-authors-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Search for an author by name or ID or query")
    public ResponseEntity<Map<String, Object>> getAuthor(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        if (id != null) {
            Optional<Author> author = authorUseCase.findAuthorById(id);
            return author.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Author not found")));

        } else if (name != null && !name.isBlank()) {
            Optional<Author> author = authorUseCase.getAuthorByName(name);
            return author.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Author with the given name not found")));

        } else if (query != null && !query.isBlank()) {
            PageRequest pageable = pageRequest(page, size, sortBy);
            Page<Author> authors = authorUseCase.searchAuthors(query, pageable);

            HttpHeaders headers = pageLinks(authors, p -> methodOn(AuthorController.class)
                    .getAuthor(null, null, query, p, size, sortBy));

            if (authors.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .headers(headers)
                        .body(Map.of("message", "No authors found for the given query"));
            }

            return ResponseEntity.ok().headers(headers).body(pageBody(authors));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No search criteria provided"));
        }
    }

    @GetMapping(value = "/paginated",
            produces = {"application/paginated-authors-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Get all authors")
    public ResponseEntity<Map<String, Object>> getAllAuthors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        PageRequest pageable = pageRequest(page, size, sortBy);
        Page<Author> authors = authorUseCase.getPaginatedAuthors(pageable);

        HttpHeaders headers = pageLinks(authors, p -> methodOn(AuthorController.class).getAllAuthors(p, size, sortBy));

        if (authors.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "There are no authors on this page.",
                    "currentPage", page,
                    "pageSize", size,
                    "sortBy", sortBy
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(errorResponse);
        }

        return ResponseEntity.ok().headers(headers).body(pageBody(authors));
    }
}
