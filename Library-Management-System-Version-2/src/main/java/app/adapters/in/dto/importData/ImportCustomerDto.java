package app.adapters.in.dto.importData;

import com.google.gson.annotations.Expose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ImportCustomerDto {
    @Expose
    @NotNull(message = "Name is required")
    private String name;

    @Expose
    @Email(message = "Email should be valid")
    @NotNull(message = "Email is required")
    private String email;

    @Expose
    private Boolean isActive;
}
