package app.adapters.input.rest;

import app.domain.dto.CreateNewAuthor;
import app.domain.models.Author;
import app.domain.port.input.AuthorUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/authors")
@Tag(name = "Author Controller", description = "Endpoints for managing authors")
public class AuthorController {
    private static final Logger log = LoggerFactory.getLogger(AuthorController.class);

    private final AuthorUseCase authorUseCase;

    @Autowired
    public AuthorController(AuthorUseCase authorUseCase) {
        this.authorUseCase = authorUseCase;
    }

    @Operation(
            summary = "Create a new author",
            description = "Creates a new author in the system",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Author created successfully",
                            content = @Content(schema = @Schema(implementation = Author.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )

    @PostMapping(produces = "application/single-book-response+json;version=1")
    public ResponseEntity<Author> createNewAuthor(@Valid @RequestBody CreateNewAuthor newAuthor) {
        log.info("Creating new author: {}", newAuthor.getName());
        Author author = authorUseCase.createNewAuthor(newAuthor);
        log.info("Author created with ID: {}", author.getAuthorId());
        return ResponseEntity.ok(author);
    }

    @Operation(
            summary = "Get an author by ID",
            description = "Retrieves a single author by UUID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Author retrieved successfully",
                            content = @Content(schema = @Schema(implementation = Author.class))),
                    @ApiResponse(responseCode = "404", description = "Author not found")
            }
    )


    @GetMapping(value = "/{id}", produces = {"application/single-author-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> getAuthorById(@PathVariable UUID id) {
        log.info("Fetching author with ID: {}", id);
        Optional<Author> authorOpt = authorUseCase.findAuthorById(id);

        if (authorOpt.isEmpty()) {
            log.warn("Author not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Author not found", "authorId", id));
        }

        CacheControl cacheControl = CacheControl
                .maxAge(30, TimeUnit.SECONDS)
                .cachePrivate()
                .noTransform();

        log.info("Author retrieved successfully: {}", id);
        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("Vary", "Accept")
                .body(Map.of("message", "Author retrieved successfully", "data", authorOpt.get()));
    }

    @Operation(
            summary = "Search authors",
            description = "Search authors by name or query with pagination",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authors retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "No authors found"),
                    @ApiResponse(responseCode = "400", description = "Invalid search criteria")
            }
    )

    @GetMapping(value = "/search", produces = "application/paginated-authors-response+json;version=1")
    public ResponseEntity<Map<String, Object>> getAuthor(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String query,
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<String> sortBy
    ) {
        log.info("Searching for author with id: {}, name: {}, query: {}", id, name, query);
        if (id != null) {
            log.info("Searching for author with id: {}", id);
            Optional<Author> author = authorUseCase.findAuthorById(id);
            return author.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Author not found")));

        } else if (name != null && !name.isBlank()) {
            log.info("Searching for author with name: {}", name);
            Optional<Author> author = authorUseCase.getAuthorByName(name);
            return author.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Author with the given name not found")));

        } else if (query != null && !query.isBlank()) {
            log.info("Searching for authors with query: {}", query);
            int currentPage = page.orElse(0);
            int pageSize = size.orElse(3);
            String sortField = sortBy.orElse("name");

            PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, sortField);
            Page<Author> authors = authorUseCase.searchAuthors(query, pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("self", "<" + linkTo(methodOn(AuthorController.class)
                    .getAuthor(null, null, query, Optional.of(currentPage), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"self\"");

            if (authors.hasPrevious()) {
                log.info("Previous page available");
                headers.add("prev", "<" + linkTo(methodOn(AuthorController.class)
                        .getAuthor(null, null, query, Optional.of(currentPage - 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"prev\"");
            }

            if (authors.hasNext()) {
                log.info("Next page available");
                headers.add("next", "<" + linkTo(methodOn(AuthorController.class)
                        .getAuthor(null, null, query, Optional.of(currentPage + 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"next\"");
            }

            if (authors.isEmpty()) {
                log.error("No authors found for the given query");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .headers(headers)
                        .body(Map.of("message", "No authors found for the given query"));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", authors.getContent());
            response.put("totalPages", authors.getTotalPages());
            response.put("currentPage", authors.getNumber());
            response.put("totalItems", authors.getTotalElements());

            return ResponseEntity.ok().headers(headers).body(response);
        } else {
            log.error("Invalid search criteria provided");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No search criteria provided"));
        }
    }

    @Operation(
            summary = "Get all authors (paginated)",
            description = "Returns all authors with pagination",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authors retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "No authors found")
            }
    )

    @GetMapping(value = "/paginated", produces = "application/paginated-authors-response+json;version=1")
    public ResponseEntity<Map<String, Object>> getAllAuthors(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<String> sortBy
    ) {
        int currentPage = page.orElse(0);
        int pageSize = size.orElse(3);
        String sortField = sortBy.orElse("name");

        PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, sortField);
        Page<Author> authors = authorUseCase.getPaginatedAuthors(pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("self", "<" + linkTo(methodOn(AuthorController.class)
                .getAllAuthors(Optional.of(currentPage), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"self\"");

        if (authors.hasNext()) {
            log.info("Next page available");
            headers.add("next", "<" + linkTo(methodOn(AuthorController.class)
                    .getAllAuthors(Optional.of(currentPage + 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"next\"");
        }

        if (authors.hasPrevious()) {
            log.info("Previous page available");
            headers.add("prev", "<" + linkTo(methodOn(AuthorController.class)
                    .getAllAuthors(Optional.of(currentPage - 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"prev\"");
        }

        if (authors.isEmpty()) {
            log.error("No authors found");
            Map<String, Object> errorResponse = Map.of(
                    "message", "There are no authors on this page.",
                    "currentPage", currentPage,
                    "pageSize", pageSize,
                    "sortBy", sortField
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(errorResponse);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", authors.getContent());
        response.put("totalPages", authors.getTotalPages());
        response.put("currentPage", authors.getNumber());
        response.put("totalItems", authors.getTotalElements());

        log.info("Authors retrieved successfully");
        return ResponseEntity.ok().headers(headers).body(response);
    }

    @Operation(
            summary = "Update an author",
            description = "Updates an existing author's details",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Author updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )

    @PutMapping(value = "/{authorId}", produces = "application/single-book-response+json;version=1")
    public ResponseEntity<String> updateAuthor(@NotNull @PathVariable UUID authorId, @Valid @RequestBody Author author) {
        log.info("Updating author with ID: {}", authorId);
        author.setAuthorId(authorId);
        authorUseCase.updateAuthor(authorId, author);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Author updated successfully!");
    }
}
