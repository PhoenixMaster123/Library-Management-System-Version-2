package app.infrastructure.config.security;

import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Resolves the signed-in account to the library membership it acts on behalf of. */
@Component
@RequiredArgsConstructor
public class CurrentAccount {

    private final UserRepository userRepository;

    /** Empty for staff accounts such as the seeded admin, which hold no membership. */
    public Optional<UUID> customerId(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName())
                .map(UserEntity::getCustomerId);
    }
}
