package pl.servicedesk.billing.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import pl.servicedesk.billing.domain.PaymentMethod;

public record RecordPaymentRequest(
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @NotNull PaymentMethod method,
        @Size(max = 100) String reference) {
}
