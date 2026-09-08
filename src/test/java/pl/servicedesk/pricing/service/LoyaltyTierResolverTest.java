package pl.servicedesk.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.pricing.PricingProperties;
import pl.servicedesk.pricing.domain.LoyaltyTier;
import pl.servicedesk.pricing.repository.LoyaltyTierRepository;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class LoyaltyTierResolverTest {

    @Mock
    private LoyaltyTierRepository tierRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private LoyaltyTierResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new LoyaltyTierResolver(tierRepository, appointmentRepository,
                new PricingProperties(new BigDecimal("25.00"), 12),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void picksTheHighestTierTheClientQualifiesFor() {
        Client client = client();
        given(appointmentRepository.countByClientIdAndStatus(1L, AppointmentStatus.COMPLETED)).willReturn(6L);
        given(appointmentRepository.sumTotalAmountByClientSince(eq(1L), eq(AppointmentStatus.COMPLETED), any()))
                .willReturn(new BigDecimal("800.00"));
        given(tierRepository.findAllByOrderByDisplayOrderAsc()).willReturn(ladder());

        assertThat(resolver.resolveFor(client)).map(LoyaltyTier::getName).contains("SILVER");
    }

    @Test
    void promotesToGoldWhenBothThresholdsAreMet() {
        Client client = client();
        given(appointmentRepository.countByClientIdAndStatus(1L, AppointmentStatus.COMPLETED)).willReturn(12L);
        given(appointmentRepository.sumTotalAmountByClientSince(eq(1L), eq(AppointmentStatus.COMPLETED), any()))
                .willReturn(new BigDecimal("3000.00"));
        given(tierRepository.findAllByOrderByDisplayOrderAsc()).willReturn(ladder());

        assertThat(resolver.resolveFor(client)).map(LoyaltyTier::getName).contains("GOLD");
    }

    @Test
    void keepsClientAtEntryTierWhenSpendIsTooLow() {
        Client client = client();
        given(appointmentRepository.countByClientIdAndStatus(1L, AppointmentStatus.COMPLETED)).willReturn(20L);
        given(appointmentRepository.sumTotalAmountByClientSince(eq(1L), eq(AppointmentStatus.COMPLETED), any()))
                .willReturn(new BigDecimal("100.00"));
        given(tierRepository.findAllByOrderByDisplayOrderAsc()).willReturn(ladder());

        assertThat(resolver.resolveFor(client)).map(LoyaltyTier::getName).contains("BRONZE");
    }

    @Test
    void returnsNothingForAnUnsavedClient() {
        assertThat(resolver.resolveFor(new Client(User.activeUser("c@x.pl", "h", "A", "B", null), false)))
                .isEmpty();
    }

    private static Client client() {
        return TestEntities.withId(new Client(User.activeUser("c@x.pl", "h", "A", "B", null), false), 1L);
    }

    private static List<LoyaltyTier> ladder() {
        return List.of(
                new LoyaltyTier("BRONZE", 0, new BigDecimal("0.00"), new BigDecimal("0.00"), 1),
                new LoyaltyTier("SILVER", 3, new BigDecimal("500.00"), new BigDecimal("5.00"), 2),
                new LoyaltyTier("GOLD", 10, new BigDecimal("2000.00"), new BigDecimal("10.00"), 3));
    }
}
