package app.adapters.output.repositories;

import app.adapters.output.entity.ReminderPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Spring Data access to reminder preferences. */
@Repository
public interface ReminderPreferenceRepository extends JpaRepository<ReminderPreferenceEntity, UUID> {
}
