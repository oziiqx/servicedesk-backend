package pl.servicedesk.staff.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import pl.servicedesk.staff.domain.EmployeeWorkingHours;

public interface EmployeeWorkingHoursRepository extends JpaRepository<EmployeeWorkingHours, Long> {

    List<EmployeeWorkingHours> findByEmployeeIdOrderByDayOfWeekAscStartTimeAsc(Long employeeId);

    @Modifying
    void deleteByEmployeeId(Long employeeId);
}
