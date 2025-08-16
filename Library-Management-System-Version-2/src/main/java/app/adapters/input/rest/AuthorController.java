package app.adapters.input.rest;

import app.domain.dto.CreateNewAuthor;
import app.domain.models.Author;
import app.domain.port.input.AuthorUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class AuthorController {
    private final AuthorUseCase authorUseCase;

    @Autowired
    public AuthorController(AuthorUseCase authorUseCase) {
        this.authorUseCase = authorUseCase;
    }

    @PostMapping(produces = "application/single-book-response+json;version=1")
    public ResponseEntity<Author> createNewAuthor(@Valid @RequestBody CreateNewAuthor newAuthor) {

        Author author = authorUseCase.createNewAuthor(newAuthor);

        return ResponseEntity.ok(author);
    }
    @GetMapping(value = "/{id}", produces = {"application/single-author-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> getAuthorById(@PathVariable UUID id) {
        Optional<Author> authorOpt = authorUseCase.findAuthorById(id);

        if (authorOpt.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "Author not found",
                    "authorId", id
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        CacheControl cacheControl = CacheControl
                .maxAge(30, TimeUnit.SECONDS)
                .cachePrivate()
                .noTransform();

        Map<String, Object> response = Map.of(
                "message", "Author retrieved successfully",
                "data", authorOpt.get()
        );

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("Vary", "Accept")
                .body(response);
    }
    @GetMapping(value = "/search", produces = "application/paginated-authors-response+json;version=1")
    public ResponseEntity<Map<String, Object>> getAuthor(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String query,
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<String> sortBy
    ) {
        if (id != null) {
            Optional<Author> author = authorUseCase.findAuthorById(id);
            return author.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Author not found")));

        } else if (name != null && !name.isBlank()) {
            Optional<Author> author = authorUseCase.getAuthorByName(name);
            return author.<ResponseEntity<Map<String, Object>>>map(value -> ResponseEntity.ok(Map.of("data", value))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Author with the given name not found")));

        } else if (query != null && !query.isBlank()) {
            int currentPage = page.orElse(0);
            int pageSize = size.orElse(3);
            String sortField = sortBy.orElse("name");

            PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, sortField);
            Page<Author> authors = authorUseCase.searchAuthors(query, pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("self", "<" + linkTo(methodOn(AuthorController.class)
                    .getAuthor(null, null, query, Optional.of(currentPage), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"self\"");

            if (authors.hasPrevious()) {
                headers.add("prev", "<" + linkTo(methodOn(AuthorController.class)
                        .getAuthor(null, null, query, Optional.of(currentPage - 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"prev\"");
            }

            if (authors.hasNext()) {
                headers.add("next", "<" + linkTo(methodOn(AuthorController.class)
                        .getAuthor(null, null, query, Optional.of(currentPage + 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"next\"");
            }

            if (authors.isEmpty()) {
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No search criteria provided"));
        }
    }

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
            headers.add("next", "<" + linkTo(methodOn(AuthorController.class)
                    .getAllAuthors(Optional.of(currentPage + 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"next\"");
        }

        if (authors.hasPrevious()) {
            headers.add("prev", "<" + linkTo(methodOn(AuthorController.class)
                    .getAllAuthors(Optional.of(currentPage - 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"prev\"");
        }

        if (authors.isEmpty()) {
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

        return ResponseEntity.ok().headers(headers).body(response);
    }
    @PutMapping(value = "/{authorId}", produces = "application/single-book-response+json;version=1")
    public ResponseEntity<String> updateAuthor(@NotNull @PathVariable UUID authorId, @Valid @RequestBody Author author) {

        author.setAuthorId(authorId);
        authorUseCase.updateAuthor(authorId, author);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Author updated successfully!");
    }
}
