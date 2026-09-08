package pl.servicedesk.staff.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import pl.servicedesk.staff.domain.EmployeeSkill;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, Long> {

    @EntityGraph(attributePaths = "serviceOffering")
    List<EmployeeSkill> findByEmployeeIdOrderByServiceOfferingNameAsc(Long employeeId);

    boolean existsByEmployeeIdAndServiceOfferingCode(Long employeeId, String serviceCode);

    @Modifying
    void deleteByEmployeeId(Long employeeId);
}
