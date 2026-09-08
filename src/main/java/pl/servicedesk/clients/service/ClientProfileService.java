package pl.servicedesk.clients.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.clients.repository.ClientRepository;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.pricing.PricingProperties;
import pl.servicedesk.pricing.domain.LoyaltyTier;
import pl.servicedesk.pricing.service.LoyaltyTierResolver;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;

@Service
@RequiredArgsConstructor
public class ClientProfileService {

    private final ClientRepository clientRepository;
    private final LoyaltyTierResolver loyaltyTierResolver;
    private final AppointmentRepository appointmentRepository;
    private final PricingProperties pricingProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ClientProfile forUser(Long userId) {
        Client client = clientRepository.findWithUserByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("The current account is not a client"));

        long completed = appointmentRepository.countByClientIdAndStatus(
                client.getId(), AppointmentStatus.COMPLETED);
        Instant since = ZonedDateTime.now(clock)
                .minusMonths(pricingProperties.loyaltySpendWindowMonths())
                .toInstant();
        BigDecimal lifetimeSpend = appointmentRepository.sumTotalAmountByClientSince(
                client.getId(), AppointmentStatus.COMPLETED, since);
        String tier = loyaltyTierResolver.resolveFor(client).map(LoyaltyTier::getName).orElse(null);

        return new ClientProfile(
                client.getUser().getEmail(),
                client.getUser().fullName(),
                client.isMarketingConsent(),
                tier,
                completed,
                lifetimeSpend);
    }

    public record ClientProfile(
            String email,
            String fullName,
            boolean marketingConsent,
            String loyaltyTier,
            long completedAppointments,
            BigDecimal lifetimeSpend) {
    }
}
