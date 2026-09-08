package pl.servicedesk.pricing.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.pricing.domain.PricingRule;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<PricingRule> findByCodeIgnoreCase(String code);

    List<PricingRule> findAllByOrderByPriorityAscCodeAsc();

    List<PricingRule> findAllByActiveTrueOrderByPriorityAsc();
}
