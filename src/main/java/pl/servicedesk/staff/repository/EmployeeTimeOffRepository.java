package pl.servicedesk.staff.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.staff.domain.EmployeeTimeOff;

public interface EmployeeTimeOffRepository extends JpaRepository<EmployeeTimeOff, Long> {

    List<EmployeeTimeOff> findByEmployeeIdAndEndsAtAfterOrderByStartsAtAsc(Long employeeId, Instant moment);

    Optional<EmployeeTimeOff> findByIdAndEmployeeId(Long id, Long employeeId);

    boolean existsByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(Long employeeId, Instant end, Instant start);

    List<EmployeeTimeOff> findByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(
            Long employeeId, Instant end, Instant start);
}
