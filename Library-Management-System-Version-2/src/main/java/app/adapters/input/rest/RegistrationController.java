package app.adapters.input.rest;

import app.adapters.output.entity.UserEntity;
import app.adapters.output.repositories.UserRepository;
import app.domain.dto.CreateNewCustomer;
import app.domain.dto.RegisterRequest;
import app.domain.model.Customer;
import app.domain.port.input.CustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Self-registration, which creates an account and its membership together. Members only. */
@RestController
@RequestMapping("/api")
@Tag(name = "Registration Controller", description = "Endpoint for opening a library account")
@RequiredArgsConstructor
public class RegistrationController {

    /** Everyone who signs up themselves is a library member, never an administrator. */
    private static final String MEMBER_ROLE = "USER";

    private final UserRepository userRepository;
    private final CustomerUseCase customerUseCase;
    private final PasswordEncoder passwordEncoder;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an account and the matching library membership")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request,
                                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", firstErrorMessage(bindingResult)));
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username is already taken"));
        }

        // The account and the membership are created together: the customer row is what an
        // administrator later sees under /customers.
        Customer customer = customerUseCase.createNewCustomer(
                new CreateNewCustomer(request.getName(), request.getEmail(), true));

        userRepository.save(new UserEntity(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                MEMBER_ROLE,
                customer.getCustomerId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Account created successfully",
                "username", request.getUsername(),
                "customerId", customer.getCustomerId()
        ));
    }

    private String firstErrorMessage(BindingResult bindingResult) {
        FieldError error = bindingResult.getFieldError();
        return (error == null || error.getDefaultMessage() == null)
                ? "Invalid registration data"
                : error.getDefaultMessage();
    }
}
