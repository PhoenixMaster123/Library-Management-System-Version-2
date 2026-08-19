package app.infrastructure.exceptions;

/** Raised when an author id matches nothing. */
public class AuthorNotFoundException extends ResourceNotFoundException {
    public AuthorNotFoundException(String message) {
        super(message);
    }
}
