package pl.servicedesk.pricing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.common.domain.AuditableEntity;

@Entity
@Getter
@Table(name = "loyalty_tiers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyTier extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "min_completed_appointments", nullable = false)
    private int minCompletedAppointments;

    @Column(name = "min_lifetime_spend", nullable = false, precision = 12, scale = 2)
    private BigDecimal minLifetimeSpend;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public LoyaltyTier(String name, int minCompletedAppointments, BigDecimal minLifetimeSpend,
                       BigDecimal discountPercentage, int displayOrder) {
        this.name = name;
        this.minCompletedAppointments = minCompletedAppointments;
        this.minLifetimeSpend = minLifetimeSpend;
        this.discountPercentage = discountPercentage;
        this.displayOrder = displayOrder;
    }

    public boolean isReachedBy(long completedAppointments, BigDecimal lifetimeSpend) {
        return completedAppointments >= minCompletedAppointments
                && lifetimeSpend.compareTo(minLifetimeSpend) >= 0;
    }

    public void update(int minCompletedAppointments, BigDecimal minLifetimeSpend,
                       BigDecimal discountPercentage, int displayOrder) {
        this.minCompletedAppointments = minCompletedAppointments;
        this.minLifetimeSpend = minLifetimeSpend;
        this.discountPercentage = discountPercentage;
        this.displayOrder = displayOrder;
    }
}
