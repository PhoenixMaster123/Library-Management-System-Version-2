package app.infrastructure.exceptions;

/** Raised when a book id matches nothing. */
public class BookNotFoundException extends ResourceNotFoundException {
    /** Names the book that could not be found. */
    public BookNotFoundException(String message) {
        super(message);
    }
}
