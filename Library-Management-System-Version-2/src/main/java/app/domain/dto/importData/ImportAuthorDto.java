package app.domain.dto.importData;

import com.google.gson.annotations.Expose;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
