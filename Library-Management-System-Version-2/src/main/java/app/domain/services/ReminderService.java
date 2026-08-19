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

/**
 * Due-date reminders, stored locally and mirrored to Notification-Service.
 *
 * <p>The order matters: the local row is written first and the mirror second. Notification-Service
 * being unreachable then costs the member nothing - their choice is already saved, and the next
 * change re-sends it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ReminderService implements ReminderUseCase {

    private final ReminderPreferencePort reminderPreferencePort;
    private final CustomerRepositoryPort customerRepositoryPort;
    private final NotificationPort notificationPort;

    @Override
    public ReminderSetting settingFor(UUID customerId) {
        return new ReminderSetting(reminderPreferencePort.isEnabled(customerId), emailOf(customerId));
    }

    @Override
    public ReminderSetting updateSetting(UUID customerId, boolean enabled) {
        String email = emailOf(customerId);

        reminderPreferencePort.setEnabled(customerId, enabled);
        notificationPort.saveReminderSetting(customerId, new ReminderSetting(enabled, email));

        return new ReminderSetting(enabled, email);
    }

    private String emailOf(UUID customerId) {
        return customerRepositoryPort.getCustomer(customerId)
                .map(Customer::getEmail)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found."));
    }
}
