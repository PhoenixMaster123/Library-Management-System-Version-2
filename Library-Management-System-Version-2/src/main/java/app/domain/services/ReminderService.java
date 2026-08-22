package app.domain.services;

import app.domain.model.Customer;
import app.domain.model.ReminderSetting;
import app.domain.port.input.ReminderUseCase;
import app.domain.port.output.CustomerRepositoryPort;
import app.domain.port.output.NotificationPort;
import app.domain.port.output.ReminderPreferencePort;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Due-date reminders: saved locally first, then mirrored outward, so a mirror failure costs nothing. */
@Service
@Transactional
@RequiredArgsConstructor
public class ReminderService implements ReminderUseCase {

    private final ReminderPreferencePort reminderPreferencePort;
    private final CustomerRepositoryPort customerRepositoryPort;
    private final NotificationPort notificationPort;

    /** The member's current choice, with the address reminders would go to. */
    @Override
    public ReminderSetting settingFor(UUID customerId) {
        return new ReminderSetting(reminderPreferencePort.isEnabled(customerId), emailOf(customerId));
    }

    /** Turns reminders on or off, saving locally before mirroring outward. */
    @Override
    public ReminderSetting updateSetting(UUID customerId, boolean enabled) {
        String email = emailOf(customerId);

        reminderPreferencePort.setEnabled(customerId, enabled);
        notificationPort.saveReminderSetting(customerId, new ReminderSetting(enabled, email));

        return new ReminderSetting(enabled, email);
    }

    /** The member's address; throws when there is no such member. */
    private String emailOf(UUID customerId) {
        return customerRepositoryPort.getCustomer(customerId)
                .map(Customer::getEmail)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));
    }
}
