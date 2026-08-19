package app.adapters.input.rest;

import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.UserRepository;
import app.domain.dto.ChangePasswordRequest;
import app.domain.model.AccountCredentials;
import app.domain.services.JwtService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Signing in and out, and reporting who the caller is. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginController {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String DEFAULT_ROLE = "USER";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> getToken(@RequestBody AccountCredentials credentials) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        credentials.getUsername(), credentials.getPassword()
                )
        );

        String jwt = jwtService.getToken(auth.getName(), roleOf(auth));

        Map<String, Object> body = identity(auth);
        body.put("message", "Login successful");
        body.put("token", "Bearer " + jwt);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization")
                .body(body);
    }

    /** Lets the client check on start-up whether its stored token is still valid, and who it belongs to. */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(Authentication auth) {
        return ResponseEntity.ok(identity(auth));
    }

    /**
     * The current password is required so that a stolen token cannot be used to lock the owner
     * out of their own account.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                              BindingResult bindingResult,
                                                              Authentication auth) {
        if (bindingResult.hasErrors()) {
            FieldError error = bindingResult.getFieldError();
            return ResponseEntity.badRequest().body(Map.of("message",
                    error == null || error.getDefaultMessage() == null
                            ? "Invalid password data"
                            : error.getDefaultMessage()));
        }

        UserEntity user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("Account not found."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Your current password is not correct."));
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "The new password must differ from the current one."));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    /**
     * The role decides which screens the client offers; the customerId is what it borrows with,
     * and is absent for staff accounts that have no library membership.
     */
    private Map<String, Object> identity(Authentication auth) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("username", auth.getName());
        identity.put("role", roleOf(auth));
        userRepository.findByUsername(auth.getName())
                .filter(user -> user.getCustomerId() != null)
                .ifPresent(user -> identity.put("customerId", user.getCustomerId()));
        return identity;
    }

    private String roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(authority -> authority.startsWith(ROLE_PREFIX)
                        ? authority.substring(ROLE_PREFIX.length())
                        : authority)
                .orElse(DEFAULT_ROLE);
    }
}
