package app.domain.dto.importdata;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A loan as it appears in the seed JSON. */
@Getter
@Setter
@NoArgsConstructor
public class ImportTransactionDto {
    @Expose
    private String customerName;

    @Expose
    private String bookIsbn;

    @Expose
    private String borrowDate;

    @Expose
    private String returnDate;
}
