package app.infrastructure.config.security;

import app.adapters.output.entity.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The signed-in account as Spring Security sees it. A real UserDetails rather than a built
 * {@code User}, so the principal keeps hold of the library membership behind the account -
 * which is what borrowing acts on.
 */
@Getter
public class AccountUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final String role;
    /** Null for staff accounts, which hold no membership. */
    private final UUID customerId;

    public AccountUserDetails(UserEntity user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.customerId = user.getCustomerId();
    }

    /** Spring matches hasRole("ADMIN") against the authority "ROLE_ADMIN". */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
