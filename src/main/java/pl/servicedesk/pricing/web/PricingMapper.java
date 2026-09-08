package pl.servicedesk.pricing.web;

import java.util.List;
import org.mapstruct.Mapper;
import pl.servicedesk.pricing.domain.LoyaltyTier;
import pl.servicedesk.pricing.domain.PricingRule;
import pl.servicedesk.pricing.web.dto.LoyaltyTierResponse;
import pl.servicedesk.pricing.web.dto.PricingRuleResponse;

@Mapper
interface PricingMapper {

    LoyaltyTierResponse toResponse(LoyaltyTier tier);

    List<LoyaltyTierResponse> toTierResponses(List<LoyaltyTier> tiers);

    PricingRuleResponse toResponse(PricingRule rule);

    List<PricingRuleResponse> toRuleResponses(List<PricingRule> rules);
}
