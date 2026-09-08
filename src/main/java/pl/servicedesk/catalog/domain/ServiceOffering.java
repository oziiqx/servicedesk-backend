package pl.servicedesk.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.common.domain.AuditableEntity;
import pl.servicedesk.resources.domain.ResourceType;

@Entity
@Getter
@Table(name = "services")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceOffering extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type_required", length = 20)
    private ResourceType requiredResourceType;

    @Column(nullable = false)
    private boolean active;

    public ServiceOffering(ServiceCategory category, String code, String name, String description,
                           BigDecimal basePrice, int durationMinutes, int bufferMinutes,
                           ResourceType requiredResourceType) {
        this.category = category;
        this.code = code;
        this.name = name;
        this.description = description;
        this.requiredResourceType = requiredResourceType;
        applyPricing(basePrice);
        applyDuration(durationMinutes, bufferMinutes);
        this.active = true;
    }

    public void moveToCategory(ServiceCategory category) {
        this.category = category;
    }

    public void updateDetails(String name, String description, int durationMinutes, int bufferMinutes,
                              ResourceType requiredResourceType) {
        this.name = name;
        this.description = description;
        this.requiredResourceType = requiredResourceType;
        applyDuration(durationMinutes, bufferMinutes);
    }

    public void changeBasePrice(BigDecimal basePrice) {
        applyPricing(basePrice);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Duration slotLength() {
        return Duration.ofMinutes((long) durationMinutes + bufferMinutes);
    }

    private void applyPricing(BigDecimal basePrice) {
        if (basePrice == null || basePrice.signum() < 0) {
            throw new IllegalArgumentException("Base price must not be negative");
        }
        this.basePrice = basePrice.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void applyDuration(int durationMinutes, int bufferMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Service duration must be positive");
        }
        if (bufferMinutes < 0) {
            throw new IllegalArgumentException("Buffer must not be negative");
        }
        this.durationMinutes = durationMinutes;
        this.bufferMinutes = bufferMinutes;
    }
}
