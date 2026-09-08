package pl.servicedesk.staff.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.catalog.domain.ServiceOffering;

@Entity
@Getter
@Table(name = "employee_skills", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "service_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering serviceOffering;

    public EmployeeSkill(Employee employee, ServiceOffering serviceOffering) {
        this.employee = employee;
        this.serviceOffering = serviceOffering;
    }
}
