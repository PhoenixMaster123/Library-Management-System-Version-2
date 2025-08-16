package app.domain.dto.importData;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ImportTransactionDto {
    @Expose
    private String customerName; // or email if you prefer

    @Expose
    private String bookIsbn;

    // ISO-8601 date strings, e.g., 2025-08-08
    @Expose
    private String borrowDate;

    // Optional: if present, we will return the book on this date
    @Expose
    private String returnDate;
}
