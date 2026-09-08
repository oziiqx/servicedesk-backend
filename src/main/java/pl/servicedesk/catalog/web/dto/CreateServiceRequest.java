package pl.servicedesk.catalog.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import pl.servicedesk.resources.domain.ResourceType;

public record CreateServiceRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal basePrice,
        @Positive int durationMinutes,
        @PositiveOrZero int bufferMinutes,
        ResourceType requiredResourceType) {
}
