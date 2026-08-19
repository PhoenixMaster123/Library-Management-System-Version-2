package app.domain.services;

import app.domain.model.Transaction;
import app.domain.port.output.NotificationPort;
import app.domain.port.output.ReminderPreferencePort;
import app.domain.port.output.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Reminds members a few days before a book is due back. Sweeping for one exact due date rather
 * than a range is what keeps a member from being reminded again every morning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanReminderService {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final NotificationPort notificationPort;
    private final ReminderPreferencePort reminderPreferencePort;

    @Value("${library.reminders.days-before:3}")
    private int daysBefore;

    @Scheduled(cron = "${library.reminders.cron:0 0 8 * * *}")
    public void remindMembersOfLoansDueSoon() {
        LocalDate dueDate = LocalDate.now().plusDays(daysBefore);
        List<Transaction> due = transactionRepositoryPort.findLoansDueOn(dueDate);

        if (due.isEmpty()) {
            log.debug("No loans due on {}", dueDate);
            return;
        }

        int sent = 0;
        for (Transaction loan : due) {
            if (loan.getCustomer() == null || loan.getBook() == null) {
                continue;
            }
            // Reminders are opt-in: only members who asked for them are contacted.
            if (!reminderPreferencePort.isEnabled(loan.getCustomerId())) {
                continue;
            }

            notificationPort.notifyDueSoon(loan.getCustomer(), loan.getBook(), loan.getDueDate());
            sent++;
        }

        log.info("Sent {} due-date reminder(s) for {} loan(s) due on {}", sent, due.size(), dueDate);
    }
}
