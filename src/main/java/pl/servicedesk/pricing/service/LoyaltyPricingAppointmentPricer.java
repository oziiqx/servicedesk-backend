package pl.servicedesk.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.pricing.AppointmentPricer;
import pl.servicedesk.pricing.PricingProperties;
import pl.servicedesk.pricing.domain.LoyaltyTier;
import pl.servicedesk.pricing.service.PricingRuleEngine.RuleOutcome;

@Component
@RequiredArgsConstructor
class LoyaltyPricingAppointmentPricer implements AppointmentPricer {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PricingRuleEngine pricingRuleEngine;
    private final LoyaltyTierResolver loyaltyTierResolver;
    private final PricingProperties pricingProperties;

    @Override
    public AppointmentPrice price(Client client, List<LineItem> items, Instant slotStart) {
        BigDecimal net = items.stream()
                .map(item -> item.service().getBasePrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RuleOutcome rules = pricingRuleEngine.evaluate(slotStart);
        BigDecimal surchargeAmount = percentageOf(net, rules.surchargePercentage());
        BigDecimal subtotal = net.add(surchargeAmount);

        Optional<LoyaltyTier> tier = loyaltyTierResolver.resolveFor(client);
        BigDecimal loyaltyDiscount = tier.map(LoyaltyTier::getDiscountPercentage).orElse(BigDecimal.ZERO);
        BigDecimal effectiveDiscountPercentage = loyaltyDiscount
                .add(rules.discountPercentage())
                .min(pricingProperties.maxTotalDiscountPercentage());
        BigDecimal discountAmount = percentageOf(subtotal, effectiveDiscountPercentage);
        BigDecimal total = subtotal.subtract(discountAmount);

        return new AppointmentPrice(
                scale(net),
                scale(surchargeAmount),
                scale(effectiveDiscountPercentage),
                scale(discountAmount),
                scale(total),
                tier.map(LoyaltyTier::getName).orElse(null));
    }

    private static BigDecimal percentageOf(BigDecimal base, BigDecimal percentage) {
        return base.multiply(percentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
