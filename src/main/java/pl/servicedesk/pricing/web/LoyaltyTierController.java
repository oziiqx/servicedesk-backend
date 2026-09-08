package pl.servicedesk.pricing.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.pricing.service.LoyaltyTierService;
import pl.servicedesk.pricing.web.dto.LoyaltyTierResponse;

@RestController
@RequestMapping("/api/v1/loyalty-tiers")
@RequiredArgsConstructor
@Tag(name = "Loyalty tiers")
class LoyaltyTierController {

    private final LoyaltyTierService loyaltyTierService;
    private final PricingMapper pricingMapper;

    @GetMapping
    List<LoyaltyTierResponse> list() {
        return pricingMapper.toTierResponses(loyaltyTierService.list());
    }
}
