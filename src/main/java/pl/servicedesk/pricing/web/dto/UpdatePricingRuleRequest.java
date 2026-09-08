package pl.servicedesk.pricing.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

public record UpdatePricingRuleRequest(
        @Size(max = 255) String description,
        @NotNull @DecimalMin("-100.0") @DecimalMax("100.0") BigDecimal adjustmentPercentage,
        @Pattern(regexp = "^[1-7](,[1-7])*$", message = "must be ISO day numbers separated by commas") String daysOfWeek,
        LocalTime timeFrom,
        LocalTime timeTo,
        @PositiveOrZero int priority) {
}
