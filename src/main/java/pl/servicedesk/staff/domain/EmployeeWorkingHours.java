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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "employee_working_hours")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeWorkingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public EmployeeWorkingHours(Employee employee, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Working hours must start before they end");
        }
        this.employee = employee;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean covers(LocalTime time) {
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }
}
