package pl.servicedesk.staff.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.common.domain.AuditableEntity;

@Entity
@Getter
@Table(name = "employee_time_off")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeTimeOff extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(length = 255)
    private String reason;

    public EmployeeTimeOff(Employee employee, Instant startsAt, Instant endsAt, String reason) {
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Time off must start before it ends");
        }
        this.employee = employee;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reason = reason;
    }

    public boolean overlaps(Instant from, Instant to) {
        return startsAt.isBefore(to) && from.isBefore(endsAt);
    }
}
