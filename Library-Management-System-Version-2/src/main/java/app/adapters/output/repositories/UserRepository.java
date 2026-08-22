package app.adapters.output.repositories;

import app.adapters.output.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
/** Spring Data access to sign-in accounts. */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    /** The account with this username, or empty. */
    Optional<UserEntity> findByUsername(String username);
}
