package pl.servicedesk.pricing.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.pricing.PricingProperties;
import pl.servicedesk.pricing.domain.LoyaltyTier;
import pl.servicedesk.pricing.repository.LoyaltyTierRepository;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;

@Component
@RequiredArgsConstructor
public class LoyaltyTierResolver {

    private final LoyaltyTierRepository tierRepository;
    private final AppointmentRepository appointmentRepository;
    private final PricingProperties pricingProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Optional<LoyaltyTier> resolveFor(Client client) {
        if (client.getId() == null) {
            return Optional.empty();
        }
        long completed = appointmentRepository.countByClientIdAndStatus(
                client.getId(), AppointmentStatus.COMPLETED);
        Instant since = ZonedDateTime.now(clock)
                .minusMonths(pricingProperties.loyaltySpendWindowMonths())
                .toInstant();
        BigDecimal lifetimeSpend = appointmentRepository.sumTotalAmountByClientSince(
                client.getId(), AppointmentStatus.COMPLETED, since);

        return tierRepository.findAllByOrderByDisplayOrderAsc().stream()
                .filter(tier -> tier.isReachedBy(completed, lifetimeSpend))
                .reduce((lower, higher) -> higher);
    }
}
