package pl.servicedesk.pricing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "servicedesk.pricing")
public record PricingProperties(
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal maxTotalDiscountPercentage,
        @Positive int loyaltySpendWindowMonths) {
}
