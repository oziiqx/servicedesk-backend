package pl.servicedesk.staff.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmploymentStatus;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForUpdate(Long id);

    @EntityGraph(attributePaths = "user")
    Optional<Employee> findWithUserById(Long id);

    @EntityGraph(attributePaths = "user")
    Optional<Employee> findWithUserByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<Employee> findAllByOrderByDisplayNameAsc();

    @EntityGraph(attributePaths = "user")
    List<Employee> findAllByStatusOrderByDisplayNameAsc(EmploymentStatus status);
}
