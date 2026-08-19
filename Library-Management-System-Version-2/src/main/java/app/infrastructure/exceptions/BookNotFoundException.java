package app.infrastructure.exceptions;

/** Raised when a book id matches nothing. */
public class BookNotFoundException extends ResourceNotFoundException {
    public BookNotFoundException(String message) {
        super(message);
    }
}
