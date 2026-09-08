package pl.servicedesk.billing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "servicedesk.billing")
public record BillingProperties(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal invoiceTaxRate,
        @Positive int invoicePaymentTermDays) {
}
