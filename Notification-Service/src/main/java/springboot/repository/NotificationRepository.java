package springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import springboot.model.Notification;

import java.util.List;
import java.util.UUID;

/** Stores every notification raised, delivered or not. */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Newest first, for the history endpoint. */
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
