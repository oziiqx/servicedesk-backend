package pl.servicedesk.resources.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.servicedesk.resources.domain.ResourceType;

public record CreateResourceRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull ResourceType type) {
}
