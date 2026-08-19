package app.adapters.input.rest;

import app.domain.model.Customer;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.output.TransactionRepositoryPort;
import app.domain.services.TransactionService;
import app.infrastructure.config.security.CurrentAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What the signed-in account can be told about itself.
 *
 * <p>Separate from {@code /customers}, which is administrators only: a member reading their own
 * name and email is not the same permission as reading everybody's, and folding the two together
 * would mean opening the member list to get a settings page.
 */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile Controller", description = "The signed-in account's own details")
@RequiredArgsConstructor
public class ProfileController {

    private final CurrentAccount currentAccount;
    private final CustomerRepositoryPort customerRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "My account details")
    public ResponseEntity<Map<String, Object>> myProfile(Authentication authentication) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("username", authentication.getName());
        profile.put("role", roleOf(authentication));
        profile.put("loanLimit", TransactionService.MAX_ACTIVE_LOANS);

        Optional<UUID> customerId = currentAccount.customerId(authentication);
        if (customerId.isEmpty()) {
            // Staff accounts hold no membership, so there is no name, email or loan to report.
            profile.put("member", false);
            return ResponseEntity.ok(profile);
        }

        Optional<Customer> customer = customerRepositoryPort.getCustomer(customerId.get());
        profile.put("member", true);
        profile.put("name", customer.map(Customer::getName).orElse(""));
        profile.put("email", customer.map(Customer::getEmail).orElse(""));
        profile.put("activeLoans", transactionRepositoryPort.countActiveLoans(customerId.get()));

        return ResponseEntity.ok(profile);
    }

    private String roleOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .orElse("USER");
    }
}
