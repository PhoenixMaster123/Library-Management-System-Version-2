package app.adapters.in.dto.importData;

import com.google.gson.annotations.Expose;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ImportBookDto {

    @Expose
    @NotNull(message = "Title is required")
    private String title;

    @Expose
    @NotNull(message = "ISBN is required")
    private String isbn;

    @Expose
    @NotNull(message = "Publication year is required")
    private Integer publicationYear;

    @Expose
    @NotNull(message = "Authors list is required")
    private List<ImportAuthorDto> authors;
}
