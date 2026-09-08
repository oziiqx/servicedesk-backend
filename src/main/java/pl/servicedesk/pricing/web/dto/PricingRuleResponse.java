package pl.servicedesk.pricing.web.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record PricingRuleResponse(
        Long id,
        String code,
        String description,
        BigDecimal adjustmentPercentage,
        String daysOfWeek,
        LocalTime timeFrom,
        LocalTime timeTo,
        int priority,
        boolean active) {
}
