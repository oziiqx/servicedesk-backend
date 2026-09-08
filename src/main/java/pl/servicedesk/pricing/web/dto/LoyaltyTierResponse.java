package pl.servicedesk.pricing.web.dto;

import java.math.BigDecimal;

public record LoyaltyTierResponse(
        Long id,
        String name,
        int minCompletedAppointments,
        BigDecimal minLifetimeSpend,
        BigDecimal discountPercentage,
        int displayOrder) {
}
