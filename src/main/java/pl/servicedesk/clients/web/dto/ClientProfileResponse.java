package pl.servicedesk.clients.web.dto;

import java.math.BigDecimal;

public record ClientProfileResponse(
        String email,
        String fullName,
        boolean marketingConsent,
        String loyaltyTier,
        long completedAppointments,
        BigDecimal lifetimeSpend) {
}
