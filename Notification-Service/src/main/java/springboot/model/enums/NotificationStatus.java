package springboot.model.enums;

/** Delivery outcome of one notification. The row is always kept; this records what the send did. */
public enum NotificationStatus {
    /** Persisted, but not dispatched yet (e.g. mail delivery is disabled). */
    PENDING,
    /** Handed off to the mail server without error. */
    SUCCEEDED,
    /** Dispatch was attempted and failed; see {@code failureReason}. */
    FAILED
}
