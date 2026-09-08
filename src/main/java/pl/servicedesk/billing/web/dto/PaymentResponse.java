package pl.servicedesk.billing.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        String method,
        Instant paidAt,
        String reference) {
}
