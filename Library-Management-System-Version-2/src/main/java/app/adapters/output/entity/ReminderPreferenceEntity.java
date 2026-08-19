package app.adapters.output.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A member's due-date reminder choice, keyed by their membership.
 *
 * <p>The authoritative copy: Notification-Service is told about changes but never read back from.
 */
@Entity
@Table(name = "reminder_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderPreferenceEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID customerId;

    @Column(nullable = false)
    private boolean enabled;
}
