package app.infrastructure.exceptions;

/** A record the caller asked for does not exist. The handler answers 404 on this base type. */
public abstract class ResourceNotFoundException extends RuntimeException {

    /** Subclasses pass the message the caller will see. */
    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
