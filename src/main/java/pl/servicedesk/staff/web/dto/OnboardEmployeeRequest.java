package pl.servicedesk.staff.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record OnboardEmployeeRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 32) @Pattern(regexp = "^[+0-9 ()-]*$", message = "must contain digits and phone symbols only") String phone,
        @NotBlank @Size(max = 150) String displayName,
        @Size(max = 100) String title,
        @NotNull @PastOrPresent LocalDate hiredOn) {
}
