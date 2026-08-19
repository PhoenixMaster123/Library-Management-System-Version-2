package app.adapters.input.rest;

import app.domain.model.ReminderSetting;
import app.domain.port.input.ReminderUseCase;
import app.infrastructure.config.security.CurrentAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lets a member switch due-date reminders on and off.
 *
 * <p>There is deliberately no way to say where they go: reminders are sent to the address on the
 * membership, so the endpoint reports it but never accepts one.
 */
@RestController
@RequestMapping("/api/reminders")
@Tag(name = "Reminder Controller", description = "Due-date reminder preferences")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderUseCase reminderUseCase;
    private final CurrentAccount currentAccount;

    /** Body of the update. Any {@code email} the client sends is ignored - see the class comment. */
    public record ReminderUpdateRequest(boolean enabled) {
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "My reminder settings")
    public ResponseEntity<Map<String, Object>> mySetting(Authentication authentication) {
        Optional<UUID> customerId = currentAccount.customerId(authentication);
        if (customerId.isEmpty()) {
            return ResponseEntity.ok(Map.of("supported", false, "enabled", false, "email", ""));
        }

        ReminderSetting setting = reminderUseCase.settingFor(customerId.get());

        return ResponseEntity.ok(Map.of(
                "supported", true,
                "enabled", setting.enabled(),
                "email", setting.email() == null ? "" : setting.email()));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Turn due-date reminders on or off")
    public ResponseEntity<Map<String, Object>> updateSetting(@RequestBody ReminderUpdateRequest request,
                                                             Authentication authentication) {
        Optional<UUID> customerId = currentAccount.customerId(authentication);
        if (customerId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "This account has no library membership to remind."));
        }

        ReminderSetting saved = reminderUseCase.updateSetting(customerId.get(), request.enabled());

        return ResponseEntity.ok(Map.of(
                "message", saved.enabled()
                        ? "Reminders are on. We will write a few days before a book is due."
                        : "Reminders are off.",
                "enabled", saved.enabled(),
                "email", saved.email() == null ? "" : saved.email()));
    }
}
