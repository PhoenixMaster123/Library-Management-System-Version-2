package springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import springboot.model.NotificationPreference;

import java.util.Optional;
import java.util.UUID;

/** Stores one delivery preference per user. */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /** The user's preference, or empty when they have never set one. */
    Optional<NotificationPreference> findByUserId(UUID userId);
}
