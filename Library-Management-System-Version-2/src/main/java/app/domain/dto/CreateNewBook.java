package app.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewBook {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @Min(value = 1000, message = "Year must be valid")
    @Max(value = 9999, message = "Year must be valid")
    private int publicationYear;

    @NotNull(message = "Authors list is required")
    @Size(min = 1, message = "At least one author is required")
    @Valid
    private List<CreateNewAuthor> authors;
}
