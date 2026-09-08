package pl.servicedesk.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.pricing.domain.PricingRule;
import pl.servicedesk.pricing.repository.PricingRuleRepository;
import pl.servicedesk.pricing.service.PricingRuleEngine.RuleOutcome;
import pl.servicedesk.scheduling.BookingProperties;

@ExtendWith(MockitoExtension.class)
class PricingRuleEngineTest {

    @Mock
    private PricingRuleRepository ruleRepository;

    private PricingRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PricingRuleEngine(ruleRepository, new BookingProperties(
                "Europe/Warsaw", Duration.ofHours(1), Period.ofDays(60), Duration.ofHours(24), Duration.ofMinutes(15)));
    }

    @Test
    void appliesTheWeekendSurchargeOnASaturday() {
        given(ruleRepository.findAllByActiveTrueOrderByPriorityAsc()).willReturn(List.of(
                new PricingRule("WEEKEND", "Weekend", new BigDecimal("15.00"), "6,7", null, null, 10)));

        RuleOutcome outcome = engine.evaluate(Instant.parse("2026-06-06T10:00:00Z"));

        assertThat(outcome.surchargePercentage()).isEqualByComparingTo("15.00");
        assertThat(outcome.discountPercentage()).isEqualByComparingTo("0");
        assertThat(outcome.appliedRuleCodes()).containsExactly("WEEKEND");
    }

    @Test
    void appliesTheEveningSurchargeOnlyInsideItsTimeWindow() {
        given(ruleRepository.findAllByActiveTrueOrderByPriorityAsc()).willReturn(List.of(
                new PricingRule("EVENING", "Evening", new BigDecimal("10.00"), null,
                        LocalTime.of(18, 0), LocalTime.of(23, 59), 20)));

        Instant insideWindow = Instant.parse("2026-06-09T17:30:00Z");
        Instant beforeWindow = Instant.parse("2026-06-09T12:00:00Z");

        assertThat(engine.evaluate(insideWindow).surchargePercentage()).isEqualByComparingTo("10.00");
        assertThat(engine.evaluate(beforeWindow).surchargePercentage()).isEqualByComparingTo("0");
    }

    @Test
    void collectsDiscountRulesSeparatelyFromSurcharges() {
        given(ruleRepository.findAllByActiveTrueOrderByPriorityAsc()).willReturn(List.of(
                new PricingRule("EARLY_BIRD", "Early bird", new BigDecimal("-8.00"), null,
                        LocalTime.of(6, 0), LocalTime.of(9, 0), 5)));

        RuleOutcome outcome = engine.evaluate(Instant.parse("2026-06-09T06:00:00Z"));

        assertThat(outcome.surchargePercentage()).isEqualByComparingTo("0");
        assertThat(outcome.discountPercentage()).isEqualByComparingTo("8.00");
    }
}
