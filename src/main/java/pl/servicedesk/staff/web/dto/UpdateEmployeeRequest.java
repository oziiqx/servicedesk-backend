package pl.servicedesk.staff.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
        @NotBlank @Size(max = 150) String displayName,
        @Size(max = 100) String title) {
}
