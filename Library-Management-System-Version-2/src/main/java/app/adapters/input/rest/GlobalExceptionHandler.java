package app.adapters.input.rest;

import app.infrastructure.exceptions.BorrowNotAllowedException;
import app.infrastructure.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Turns exceptions into the JSON error bodies the API promises. */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Matching the base type, not each subclass, is what stops a new one falling through as a 500. */
    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(messageBody(ex, "Not found"));
    }

    /** A business rule refused the request - a bad due date, a copy that is already on loan. */
    @ExceptionHandler({IllegalArgumentException.class, BorrowNotAllowedException.class})
    public ResponseEntity<Map<String, String>> handleInvalidRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(messageBody(ex, "Invalid request"));
    }

    /** Map.of rejects a null value, which would turn the failure into a 500 from the handler itself. */
    private Map<String, String> messageBody(Exception ex, String fallback) {
        return Map.of("message", ex.getMessage() == null ? fallback : ex.getMessage());
    }

    /** A path that matches nothing is a 404, not the catch-all's 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "We could not find what you were looking for."));
    }

    /** The last resort. The detail goes to the log, never to the caller. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Something went wrong on our side."));
    }

    /** Answers 400 with one entry per rejected field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    /** Answers 400 when a path variable will not parse, naming the value that failed. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() == UUID.class) {
            // getValue() is nullable, and Map.of rejects nulls - which would turn this 400 into a 500.
            Object provided = ex.getValue();
            Map<String, Object> errorResponse = Map.of(
                    "message", "Invalid UUID format",
                    "providedId", provided == null ? "" : provided.toString()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid request"));
    }

    /** Answers 401 for a wrong username or password. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
}
