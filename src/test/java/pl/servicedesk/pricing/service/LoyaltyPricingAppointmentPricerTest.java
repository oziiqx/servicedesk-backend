package pl.servicedesk.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.catalog.domain.ServiceCategory;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.pricing.AppointmentPricer.AppointmentPrice;
import pl.servicedesk.pricing.AppointmentPricer.LineItem;
import pl.servicedesk.pricing.PricingProperties;
import pl.servicedesk.pricing.domain.LoyaltyTier;
import pl.servicedesk.pricing.service.PricingRuleEngine.RuleOutcome;

@ExtendWith(MockitoExtension.class)
class LoyaltyPricingAppointmentPricerTest {

    private static final Instant SLOT = Instant.parse("2026-06-06T10:00:00Z");

    @Mock
    private PricingRuleEngine pricingRuleEngine;

    @Mock
    private LoyaltyTierResolver loyaltyTierResolver;

    private LoyaltyPricingAppointmentPricer pricer;

    @BeforeEach
    void setUp() {
        pricer = new LoyaltyPricingAppointmentPricer(pricingRuleEngine, loyaltyTierResolver,
                new PricingProperties(new BigDecimal("25.00"), 12));
    }

    @Test
    void chargesTheBaseRateWhenNoRulesOrTierApply() {
        given(pricingRuleEngine.evaluate(any())).willReturn(none());
        given(loyaltyTierResolver.resolveFor(any())).willReturn(Optional.empty());

        AppointmentPrice price = pricer.price(client(), List.of(line("100.00", 2)), SLOT);

        assertThat(price.netAmount()).isEqualByComparingTo("200.00");
        assertThat(price.surchargeAmount()).isEqualByComparingTo("0.00");
        assertThat(price.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(price.totalAmount()).isEqualByComparingTo("200.00");
        assertThat(price.loyaltyTier()).isNull();
    }

    @Test
    void appliesTheLoyaltyDiscountToTheSubtotal() {
        given(pricingRuleEngine.evaluate(any())).willReturn(none());
        given(loyaltyTierResolver.resolveFor(any())).willReturn(Optional.of(tier("SILVER", "5.00")));

        AppointmentPrice price = pricer.price(client(), List.of(line("100.00", 1)), SLOT);

        assertThat(price.discountPercentage()).isEqualByComparingTo("5.00");
        assertThat(price.discountAmount()).isEqualByComparingTo("5.00");
        assertThat(price.totalAmount()).isEqualByComparingTo("95.00");
        assertThat(price.loyaltyTier()).isEqualTo("SILVER");
    }

    @Test
    void addsSurchargeBeforeApplyingTheDiscount() {
        given(pricingRuleEngine.evaluate(any()))
                .willReturn(new RuleOutcome(new BigDecimal("15.00"), BigDecimal.ZERO, List.of("WEEKEND")));
        given(loyaltyTierResolver.resolveFor(any())).willReturn(Optional.of(tier("GOLD", "10.00")));

        AppointmentPrice price = pricer.price(client(), List.of(line("100.00", 1)), SLOT);

        assertThat(price.surchargeAmount()).isEqualByComparingTo("15.00");
        assertThat(price.discountAmount()).isEqualByComparingTo("11.50");
        assertThat(price.totalAmount()).isEqualByComparingTo("103.50");
    }

    @Test
    void capsTheCombinedDiscountAtTheConfiguredMaximum() {
        given(pricingRuleEngine.evaluate(any()))
                .willReturn(new RuleOutcome(BigDecimal.ZERO, new BigDecimal("10.00"), List.of("EARLY_BIRD")));
        given(loyaltyTierResolver.resolveFor(any())).willReturn(Optional.of(tier("GOLD", "20.00")));

        AppointmentPrice price = pricer.price(client(), List.of(line("100.00", 1)), SLOT);

        assertThat(price.discountPercentage()).isEqualByComparingTo("25.00");
        assertThat(price.totalAmount()).isEqualByComparingTo("75.00");
    }

    private static RuleOutcome none() {
        return new RuleOutcome(BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }

    private static LineItem line(String price, int quantity) {
        ServiceOffering service = new ServiceOffering(new ServiceCategory("Cat", null, 0), "SVC", "Service", null,
                new BigDecimal(price), 30, 0, null);
        return new LineItem(service, quantity);
    }

    private static LoyaltyTier tier(String name, String discountPercentage) {
        return new LoyaltyTier(name, 0, BigDecimal.ZERO, new BigDecimal(discountPercentage), 1);
    }

    private static Client client() {
        return new Client(User.activeUser("c@example.com", "h", "A", "B", null), false);
    }
}
