package app.infrastructure.exceptions;

/** Raised when an author id matches nothing. */
public class AuthorNotFoundException extends ResourceNotFoundException {
    /** Names the author that could not be found. */
    public AuthorNotFoundException(String message) {
        super(message);
    }
}
