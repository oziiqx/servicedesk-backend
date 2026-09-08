package pl.servicedesk.pricing.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.pricing.domain.LoyaltyTier;
import pl.servicedesk.pricing.repository.LoyaltyTierRepository;

@Service
@RequiredArgsConstructor
public class LoyaltyTierService {

    private final LoyaltyTierRepository tierRepository;

    @Transactional(readOnly = true)
    public List<LoyaltyTier> list() {
        return tierRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public LoyaltyTier create(String name, int minCompletedAppointments, BigDecimal minLifetimeSpend,
                              BigDecimal discountPercentage, int displayOrder) {
        String normalized = name.trim();
        if (tierRepository.existsByNameIgnoreCase(normalized)) {
            throw new DuplicateResourceException("Loyalty tier '%s' already exists".formatted(normalized));
        }
        return tierRepository.save(new LoyaltyTier(
                normalized, minCompletedAppointments, minLifetimeSpend, discountPercentage, displayOrder));
    }

    @Transactional
    public LoyaltyTier update(Long id, int minCompletedAppointments, BigDecimal minLifetimeSpend,
                              BigDecimal discountPercentage, int displayOrder) {
        LoyaltyTier tier = tierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Loyalty tier", id));
        tier.update(minCompletedAppointments, minLifetimeSpend, discountPercentage, displayOrder);
        return tier;
    }
}
