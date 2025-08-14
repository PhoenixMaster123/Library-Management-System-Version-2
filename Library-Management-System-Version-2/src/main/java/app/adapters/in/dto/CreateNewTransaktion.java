package app.adapters.in.dto;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateNewTransaktion {
    @NotNull(message = "Borrow date is required")
    @FutureOrPresent(message = "Borrow date must be today or in the future")
    private LocalDate borrowDate;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Book ID is required")
    private UUID bookId;
}
