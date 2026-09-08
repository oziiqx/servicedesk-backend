package pl.servicedesk.scheduling.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.common.domain.AuditableEntity;
import pl.servicedesk.common.error.InvalidAppointmentStateException;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.staff.domain.Employee;

@Entity
@Getter
@Table(name = "appointments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private BookableResource resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private Instant scheduledEnd;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AppointmentItem> items = new ArrayList<>();

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "surcharge_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal surchargeAmount;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "loyalty_tier_at_booking", length = 20)
    private String loyaltyTierAtBooking;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    @Version
    private long version;

    private Appointment(Client client, Employee employee, BookableResource resource,
                        Instant scheduledStart, Instant scheduledEnd) {
        this.client = client;
        this.employee = employee;
        this.resource = resource;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.status = AppointmentStatus.PENDING;
    }

    public static Appointment schedule(Client client, Employee employee, BookableResource resource,
                                       Instant scheduledStart, Instant scheduledEnd) {
        if (!scheduledStart.isBefore(scheduledEnd)) {
            throw new IllegalArgumentException("Appointment must start before it ends");
        }
        return new Appointment(client, employee, resource, scheduledStart, scheduledEnd);
    }

    public void addItem(ServiceOffering service, int quantity) {
        items.add(new AppointmentItem(this, service, quantity));
    }

    public void applyPricing(BigDecimal netAmount, BigDecimal surchargeAmount, BigDecimal discountPercentage,
                             BigDecimal discountAmount, BigDecimal totalAmount, String loyaltyTierAtBooking) {
        this.netAmount = netAmount;
        this.surchargeAmount = surchargeAmount;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.loyaltyTierAtBooking = loyaltyTierAtBooking;
    }

    public void transitionTo(AppointmentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidAppointmentStateException(
                    "Cannot move appointment %d from %s to %s".formatted(id, status, target));
        }
        this.status = target;
    }

    public void cancel(String reason, Long byUserId, Instant when) {
        transitionTo(AppointmentStatus.CANCELLED);
        this.cancellationReason = reason;
        this.cancelledByUserId = byUserId;
        this.cancelledAt = when;
    }

    public void rescheduleTo(Instant newStart, Instant newEnd) {
        if (status != AppointmentStatus.PENDING && status != AppointmentStatus.CONFIRMED) {
            throw new InvalidAppointmentStateException(
                    "Only pending or confirmed appointments can be rescheduled");
        }
        if (!newStart.isBefore(newEnd)) {
            throw new IllegalArgumentException("Appointment must start before it ends");
        }
        this.scheduledStart = newStart;
        this.scheduledEnd = newEnd;
    }

    public Duration duration() {
        return Duration.between(scheduledStart, scheduledEnd);
    }

    public boolean isOwnedBy(Long clientId) {
        return client.getId().equals(clientId);
    }

    public List<AppointmentItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
