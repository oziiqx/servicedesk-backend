package pl.servicedesk.pricing.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.pricing.domain.PricingRule;
import pl.servicedesk.pricing.repository.PricingRuleRepository;

@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final PricingRuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public List<PricingRule> list() {
        return ruleRepository.findAllByOrderByPriorityAscCodeAsc();
    }

    @Transactional
    public PricingRule create(RuleData data) {
        String code = data.code().trim();
        if (ruleRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Pricing rule '%s' already exists".formatted(code));
        }
        validateWindow(data.timeFrom(), data.timeTo());
        return ruleRepository.save(new PricingRule(code, trimToNull(data.description()),
                data.adjustmentPercentage(), trimToNull(data.daysOfWeek()),
                data.timeFrom(), data.timeTo(), data.priority()));
    }

    @Transactional
    public PricingRule update(Long id, RuleData data) {
        PricingRule rule = require(id);
        validateWindow(data.timeFrom(), data.timeTo());
        rule.redefine(trimToNull(data.description()), data.adjustmentPercentage(), trimToNull(data.daysOfWeek()),
                data.timeFrom(), data.timeTo(), data.priority());
        return rule;
    }

    @Transactional
    public PricingRule changeActivation(Long id, boolean active) {
        PricingRule rule = require(id);
        if (active) {
            rule.activate();
        } else {
            rule.deactivate();
        }
        return rule;
    }

    private PricingRule require(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Pricing rule", id));
    }

    private void validateWindow(LocalTime from, LocalTime to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BusinessRuleViolationException("Rule window start must be before its end");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record RuleData(
            String code,
            String description,
            BigDecimal adjustmentPercentage,
            String daysOfWeek,
            LocalTime timeFrom,
            LocalTime timeTo,
            int priority) {
    }
}
