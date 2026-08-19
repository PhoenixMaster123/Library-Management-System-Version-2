package app.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** The details needed to put a book on the shelves. */
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

    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    private String description;

    @NotNull(message = "Authors list is required")
    @Size(min = 1, message = "At least one author is required")
    @Valid
    private List<CreateNewAuthor> authors;

    /** Most books are added without a blurb; the catalogue lookup fills one in when it has one. */
    public CreateNewBook(String title, String isbn, int publicationYear, List<CreateNewAuthor> authors) {
        this(title, isbn, publicationYear, null, authors);
    }
}
