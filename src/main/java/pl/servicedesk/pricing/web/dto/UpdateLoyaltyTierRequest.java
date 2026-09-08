package pl.servicedesk.pricing.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateLoyaltyTierRequest(
        @PositiveOrZero int minCompletedAppointments,
        @NotNull @PositiveOrZero BigDecimal minLifetimeSpend,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal discountPercentage,
        @PositiveOrZero int displayOrder) {
}
