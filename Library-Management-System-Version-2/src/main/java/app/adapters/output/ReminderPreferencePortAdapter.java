package app.adapters.output;

import app.adapters.output.entity.ReminderPreferenceEntity;
import app.adapters.output.repositories.ReminderPreferenceRepository;
import app.domain.port.output.ReminderPreferencePort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Persists reminder preferences through JPA. */
@Component
@RequiredArgsConstructor
@Transactional
public class ReminderPreferencePortAdapter implements ReminderPreferencePort {

    private final ReminderPreferenceRepository repository;

    /** Stores a member's reminder choice, replacing whatever was there. */
    @Override
    public void setEnabled(UUID customerId, boolean enabled) {
        // The id is the membership, so save() upserts: one row per member, however often they change
        // their mind.
        repository.save(new ReminderPreferenceEntity(customerId, enabled));
    }

    /** The member's choice, or false when they have never made one. */
    @Override
    public boolean isEnabled(UUID customerId) {
        return repository.findById(customerId)
                .map(ReminderPreferenceEntity::isEnabled)
                .orElse(false);
    }
}
