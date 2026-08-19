package app.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A username and password, as submitted at sign-in. */
@Getter
@Setter
@NoArgsConstructor
public class AccountCredentials {
    private String username;
    private String password;
}
