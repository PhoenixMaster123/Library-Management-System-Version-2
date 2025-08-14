package app.adapters.in;

import app.infrastructure.exceptions.BookNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
        ResponseEntity<String> response = globalExceptionHandler.handleBookNotFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Book not found");
    }

    @Test
    void testHandleGenericException() {
        Exception exception = new Exception("Something went wrong");
        ResponseEntity<String> response = globalExceptionHandler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("An unexpected error occurred: Something went wrong");
    }
}