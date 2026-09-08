package pl.servicedesk.staff.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.common.domain.AuditableEntity;
import pl.servicedesk.identity.domain.User;

@Entity
@Getter
@Table(name = "employees")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus status;

    @Column(name = "hired_on", nullable = false)
    private LocalDate hiredOn;

    public Employee(User user, String displayName, String title, LocalDate hiredOn) {
        this.user = user;
        this.displayName = displayName;
        this.title = title;
        this.hiredOn = hiredOn;
        this.status = EmploymentStatus.ACTIVE;
    }

    public void updateProfile(String displayName, String title) {
        this.displayName = displayName;
        this.title = title;
    }

    public void changeStatus(EmploymentStatus status) {
        this.status = status;
    }

    public boolean isBookable() {
        return status == EmploymentStatus.ACTIVE;
    }
}
