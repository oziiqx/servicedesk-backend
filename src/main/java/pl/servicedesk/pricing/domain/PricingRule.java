package pl.servicedesk.pricing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.common.domain.AuditableEntity;

@Entity
@Getter
@Table(name = "pricing_rules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PricingRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(name = "adjustment_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal adjustmentPercentage;

    @Column(name = "days_of_week", length = 20)
    private String daysOfWeek;

    @Column(name = "time_from")
    private LocalTime timeFrom;

    @Column(name = "time_to")
    private LocalTime timeTo;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active;

    public PricingRule(String code, String description, BigDecimal adjustmentPercentage, String daysOfWeek,
                       LocalTime timeFrom, LocalTime timeTo, int priority) {
        this.code = code;
        this.description = description;
        this.adjustmentPercentage = adjustmentPercentage;
        this.daysOfWeek = daysOfWeek;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.priority = priority;
        this.active = true;
    }

    public boolean appliesAt(DayOfWeek day, LocalTime localTime) {
        if (daysOfWeek != null && !parsedDays().contains(day)) {
            return false;
        }
        if (timeFrom != null && localTime.isBefore(timeFrom)) {
            return false;
        }
        return timeTo == null || localTime.isBefore(timeTo);
    }

    public boolean isSurcharge() {
        return adjustmentPercentage.signum() > 0;
    }

    public BigDecimal surchargePercentage() {
        return isSurcharge() ? adjustmentPercentage : BigDecimal.ZERO;
    }

    public BigDecimal discountPercentage() {
        return isSurcharge() ? BigDecimal.ZERO : adjustmentPercentage.negate();
    }

    public void redefine(String description, BigDecimal adjustmentPercentage, String daysOfWeek,
                         LocalTime timeFrom, LocalTime timeTo, int priority) {
        this.description = description;
        this.adjustmentPercentage = adjustmentPercentage;
        this.daysOfWeek = daysOfWeek;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.priority = priority;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private Set<DayOfWeek> parsedDays() {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        Arrays.stream(daysOfWeek.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .forEach(token -> days.add(DayOfWeek.of(Integer.parseInt(token))));
        return days;
    }
}
