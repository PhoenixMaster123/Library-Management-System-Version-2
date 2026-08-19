package app.infrastructure.exceptions;

/** Both records exist but a library rule blocks the loan, so this is a 400 and not a 500. */
public class BorrowNotAllowedException extends RuntimeException {
    public BorrowNotAllowedException(String message) {
        super(message);
    }
}
