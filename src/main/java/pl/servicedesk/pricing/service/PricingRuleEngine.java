package pl.servicedesk.pricing.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.pricing.domain.PricingRule;
import pl.servicedesk.pricing.repository.PricingRuleRepository;
import pl.servicedesk.scheduling.BookingProperties;

@Component
@RequiredArgsConstructor
public class PricingRuleEngine {

    private final PricingRuleRepository ruleRepository;
    private final BookingProperties bookingProperties;

    @Transactional(readOnly = true)
    public RuleOutcome evaluate(Instant slotStart) {
        ZonedDateTime local = slotStart.atZone(bookingProperties.zoneId());
        DayOfWeek day = local.getDayOfWeek();
        LocalTime time = local.toLocalTime();

        BigDecimal surcharge = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        List<String> applied = new ArrayList<>();

        for (PricingRule rule : ruleRepository.findAllByActiveTrueOrderByPriorityAsc()) {
            if (rule.appliesAt(day, time)) {
                surcharge = surcharge.add(rule.surchargePercentage());
                discount = discount.add(rule.discountPercentage());
                applied.add(rule.getCode());
            }
        }
        return new RuleOutcome(surcharge, discount, applied);
    }

    public record RuleOutcome(BigDecimal surchargePercentage, BigDecimal discountPercentage,
                              List<String> appliedRuleCodes) {
    }
}
