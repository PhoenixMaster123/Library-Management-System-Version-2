package app.adapters.input;

import app.adapters.input.rest.GlobalExceptionHandler;
import app.infrastructure.exceptions.AuthorNotFoundException;
import app.infrastructure.exceptions.BookNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler globalExceptionHandler;
    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleBookNotFoundException() {
        BookNotFoundException exception = new BookNotFoundException("Book not found");
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleNotFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Book not found");
    }

    /** The subclass that used to fall through to the 500 catch-all because nothing matched it. */
    @Test
    void testHandleAuthorNotFoundException() {
        AuthorNotFoundException exception = new AuthorNotFoundException("Author not found");
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleNotFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Author not found");
    }

    @Test
    void testHandleNotFoundWithoutMessage() {
        ResponseEntity<Map<String, String>> response =
                globalExceptionHandler.handleNotFound(new BookNotFoundException(null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Not found");
    }

    /**
     * The message is deliberately generic. An exception's own text can carry a query, a file path
     * or a class name, and the caller has no business seeing any of it - the log does.
     */
    @Test
    void testHandleGenericException() {
        Exception exception = new Exception("Table CUSTOMERS not found in schema PUBLIC");
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "Something went wrong on our side.");
        assertThat(response.getBody().toString()).doesNotContain("CUSTOMERS");
    }
}