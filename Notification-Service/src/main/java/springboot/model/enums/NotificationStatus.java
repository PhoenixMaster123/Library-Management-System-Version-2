package springboot.model.enums;

/**
 * Delivery outcome of a single notification.
 * A notification is always persisted; the status records what happened to the send attempt.
 */
public enum NotificationStatus {
    /** Persisted, but not dispatched yet (e.g. mail delivery is disabled). */
    PENDING,
    /** Handed off to the mail server without error. */
    SUCCEEDED,
    /** Dispatch was attempted and failed; see {@code failureReason}. */
    FAILED
}
