package app.adapters.output.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable=false, updatable=false)
    private UUID id;
    @Column(nullable=false, unique=true)
    private String username;
    @Column(nullable=false)
    private String password;
    @Column(nullable=false)
    private String role;
    public UserEntity() {}
    public UserEntity(String username, String password, String role) {
        super();
        this.username = username;
        this.password = password;
        this.role = role;
    }
}

