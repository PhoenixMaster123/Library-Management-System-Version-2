package app.adapters.input.rest;

import app.infrastructure.exceptions.BorrowNotAllowedException;
import app.infrastructure.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return new ResponseEntity<>("An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
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
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
}
