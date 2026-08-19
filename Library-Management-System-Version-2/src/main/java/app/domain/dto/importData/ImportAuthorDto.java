package app.domain.dto.importdata;

import com.google.gson.annotations.Expose;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An author as it appears in the seed JSON. */
@Getter
@Setter
@NoArgsConstructor
public class ImportAuthorDto {
    @Expose
    @NotNull(message = "Name is required")
    private String name;

    @Expose
    private String bio;
}
