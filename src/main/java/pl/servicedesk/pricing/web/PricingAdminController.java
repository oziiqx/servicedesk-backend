package pl.servicedesk.pricing.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.pricing.service.LoyaltyTierService;
import pl.servicedesk.pricing.service.PricingRuleService;
import pl.servicedesk.pricing.service.PricingRuleService.RuleData;
import pl.servicedesk.pricing.web.dto.CreateLoyaltyTierRequest;
import pl.servicedesk.pricing.web.dto.LoyaltyTierResponse;
import pl.servicedesk.pricing.web.dto.PricingRuleRequest;
import pl.servicedesk.pricing.web.dto.PricingRuleResponse;
import pl.servicedesk.pricing.web.dto.UpdateLoyaltyTierRequest;
import pl.servicedesk.pricing.web.dto.UpdatePricingRuleRequest;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Pricing administration")
class PricingAdminController {

    private final LoyaltyTierService loyaltyTierService;
    private final PricingRuleService pricingRuleService;
    private final PricingMapper pricingMapper;

    @GetMapping("/loyalty-tiers")
    List<LoyaltyTierResponse> listTiers() {
        return pricingMapper.toTierResponses(loyaltyTierService.list());
    }

    @PostMapping("/loyalty-tiers")
    ResponseEntity<LoyaltyTierResponse> createTier(@Valid @RequestBody CreateLoyaltyTierRequest request) {
        LoyaltyTierResponse created = pricingMapper.toResponse(loyaltyTierService.create(
                request.name(), request.minCompletedAppointments(), request.minLifetimeSpend(),
                request.discountPercentage(), request.displayOrder()));
        return ResponseEntity.created(URI.create("/api/v1/loyalty-tiers/" + created.id())).body(created);
    }

    @PutMapping("/loyalty-tiers/{id}")
    LoyaltyTierResponse updateTier(@PathVariable Long id, @Valid @RequestBody UpdateLoyaltyTierRequest request) {
        return pricingMapper.toResponse(loyaltyTierService.update(
                id, request.minCompletedAppointments(), request.minLifetimeSpend(),
                request.discountPercentage(), request.displayOrder()));
    }

    @GetMapping("/pricing-rules")
    List<PricingRuleResponse> listRules() {
        return pricingMapper.toRuleResponses(pricingRuleService.list());
    }

    @PostMapping("/pricing-rules")
    ResponseEntity<PricingRuleResponse> createRule(@Valid @RequestBody PricingRuleRequest request) {
        PricingRuleResponse created = pricingMapper.toResponse(pricingRuleService.create(new RuleData(
                request.code(), request.description(), request.adjustmentPercentage(),
                request.daysOfWeek(), request.timeFrom(), request.timeTo(), request.priority())));
        return ResponseEntity.created(URI.create("/api/v1/admin/pricing-rules/" + created.id())).body(created);
    }

    @PutMapping("/pricing-rules/{id}")
    PricingRuleResponse updateRule(@PathVariable Long id, @Valid @RequestBody UpdatePricingRuleRequest request) {
        return pricingMapper.toResponse(pricingRuleService.update(id, new RuleData(
                null, request.description(), request.adjustmentPercentage(),
                request.daysOfWeek(), request.timeFrom(), request.timeTo(), request.priority())));
    }

    @PostMapping("/pricing-rules/{id}/activation")
    PricingRuleResponse changeRuleActivation(@PathVariable Long id, @RequestParam boolean active) {
        return pricingMapper.toResponse(pricingRuleService.changeActivation(id, active));
    }
}
