package app.infrastructure.config.database;

import app.adapters.out.H2.entity.UserEntity;
import app.adapters.out.H2.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(UserRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (repository.findByUsername("user").isEmpty()) {
                String hashedPassword = passwordEncoder.encode("user");
                UserEntity user = new UserEntity("user", hashedPassword, "USER");
                repository.save(user);
                System.out.println("Default user created: username='user', password='user'");
            }
        };
    }
}
