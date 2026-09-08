package pl.servicedesk.pricing.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.pricing.domain.LoyaltyTier;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<LoyaltyTier> findByNameIgnoreCase(String name);

    List<LoyaltyTier> findAllByOrderByDisplayOrderAsc();
}
