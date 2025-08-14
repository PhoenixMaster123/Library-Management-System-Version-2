package app.adapters.in.controller;

import app.adapters.in.dto.CreateNewBook;
import app.domain.models.Book;
import app.domain.services.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/books")
public class BookController {
    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping(produces = "application/single-book-response+json;version=1")
    public ResponseEntity<Book> createNewBook(@Valid @RequestBody CreateNewBook newBook) {

        Book book = bookService.createNewBook(newBook);

        return ResponseEntity.ok(book);
    }
    @GetMapping(value = "/paginated", produces = "application/paginated-books-response+json;version=1")
    public ResponseEntity<Map<String, Object>> getAllBooks(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<String> sortBy
    ) {
        int currentPage = page.orElse(0);
        int pageSize = size.orElse(3);
        String sortField = sortBy.orElse("title");

        PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, sortField);
        Page<Book> books = bookService.getPaginatedBooks(pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("self", "<" + linkTo(methodOn(BookController.class)
                .getAllBooks(Optional.of(currentPage), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"self\"");

        if (books.hasPrevious()) {
            headers.add("prev", "<" + linkTo(methodOn(BookController.class)
                    .getAllBooks(Optional.of(currentPage - 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"prev\"");
        }
        if (books.hasNext()) {
            headers.add("next", "<" + linkTo(methodOn(BookController.class)
                    .getAllBooks(Optional.of(currentPage + 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"next\"");
        }

        if (books.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "There are no books on this page.",
                    "currentPage", currentPage,
                    "pageSize", pageSize,
                    "sortBy", sortField
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(errorResponse);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", books.getContent());
        response.put("totalPages", books.getTotalPages());
        response.put("currentPage", books.getNumber());
        response.put("totalItems", books.getTotalElements());

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @PutMapping(value = "/{id}", produces = "application/single-book-response+json;version=1")
    public ResponseEntity<String> updateBook(@NotNull @PathVariable("id") UUID id, @NotNull @RequestBody Book book) {
        Optional<Book> existingBook = bookService.searchById(id);
        if (existingBook.isEmpty()) {
            return new ResponseEntity<>("Book not found", HttpStatus.NOT_FOUND);
        }
        book.setBookId(id);
        bookService.updateBook(id, book);
        return new ResponseEntity<>("Book updated successfully", HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@NotNull @PathVariable("id") UUID bookID) {
        bookService.deleteBook(bookID);
        return new ResponseEntity<>("Book successfully deleted!!", HttpStatus.OK);
    }
    @GetMapping(value = "/{id}", produces = {"application/single-book-response+json;version=1", MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Map<String, Object>> getBookById(@PathVariable UUID id) {

        Optional<Book> book = bookService.searchById(id);

        if (book.isEmpty()) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "Book not found",
                    "bookId", id
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        CacheControl cacheControl = CacheControl
                .maxAge(30, TimeUnit.SECONDS)
                .cachePrivate()
                .noTransform();

        Map<String, Object> response = Map.of(
                "message", "Book retrieved successfully",
                "data", book
        );

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("Vary", "Accept")
                .body(response);
    }
    @GetMapping(produces = "application/single-book-response+json;version=1")
    public ResponseEntity<?> getBook(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String query,
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            @RequestParam Optional<String> sortBy
    ) {
        if (id != null) {
            Optional<Book> book = bookService.searchById(id);
            if (book.isEmpty()) {
                return new ResponseEntity<>("Book not found", HttpStatus.NOT_FOUND);
            }

            EntityModel<Book> resource = EntityModel.of(book.get());
            resource.add(linkTo(methodOn(BookController.class).getBook(id, null, null, null, null, Optional.empty(), Optional.empty(), Optional.empty())).withSelfRel());
            resource.add(linkTo(methodOn(BookController.class).updateBook(id, book.get())).withRel("update"));
            resource.add(linkTo(methodOn(BookController.class).deleteBook(id)).withRel("delete"));

            return ResponseEntity.ok(resource);
        } else if (title != null) {
            Optional<Book> book = bookService.searchBookByTitle(title);
            if (book.isEmpty()) {
                return new ResponseEntity<>("Book with the given title not found", HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok(book);
        } else if (isbn != null) {
            Optional<Book> book = bookService.searchByIsbn(isbn);
            if (book.isEmpty()) {
                return new ResponseEntity<>("Book with the given ISBN not found", HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok(book);
        } else if (author != null) {
            Optional<Book> book = bookService.searchBookByAuthors(author, true);
            if (book.isEmpty()) {
                return new ResponseEntity<>("No books found by the given author", HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok(book);
        } else if (query != null) {
            int currentPage = page.orElse(0);
            int pageSize = size.orElse(2);
            String sortField = sortBy.orElse("title");

            PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.Direction.ASC, sortField);
            Page<Book> books = bookService.searchBooks(query, pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("self", "<" + linkTo(methodOn(BookController.class)
                    .getBook(null, null, null, null, query, Optional.of(currentPage), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"self\"");

            if (books.hasPrevious()) {
                headers.add("prev", "<" + linkTo(methodOn(BookController.class)
                        .getBook(null, null, null, null, query, Optional.of(currentPage - 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"prev\"");
            }
            if (books.hasNext()) {
                headers.add("next", "<" + linkTo(methodOn(BookController.class)
                        .getBook(null, null, null, null, query, Optional.of(currentPage + 1), Optional.of(pageSize), Optional.of(sortField))).toUri() + ">; rel=\"next\"");
            }

            if (books.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body("No books found for the given query");
            }
            return ResponseEntity.ok().headers(headers).body(books.getContent());
        } else {
            return new ResponseEntity<>("No search criteria provided", HttpStatus.BAD_REQUEST);
        }
    }
}
