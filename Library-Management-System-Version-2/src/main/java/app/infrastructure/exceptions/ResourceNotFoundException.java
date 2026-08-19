package app.infrastructure.exceptions;

/**
 * A record the caller asked for does not exist. GlobalExceptionHandler answers 404 on this base
 * type, so a new not-found exception is mapped correctly the day it is written.
 */
public abstract class ResourceNotFoundException extends RuntimeException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
