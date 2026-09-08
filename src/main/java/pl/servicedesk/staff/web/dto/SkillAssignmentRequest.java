package pl.servicedesk.staff.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record SkillAssignmentRequest(
        @NotNull Set<@NotBlank String> serviceCodes) {
}
