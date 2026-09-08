package pl.servicedesk.staff.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.staff.domain.EmploymentStatus;
import pl.servicedesk.staff.service.EmployeeService;
import pl.servicedesk.staff.service.EmployeeSkillService;
import pl.servicedesk.staff.web.dto.EmployeeSkillResponse;
import pl.servicedesk.staff.web.dto.EmployeeSummaryResponse;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Staff directory")
class EmployeeDirectoryController {

    private final EmployeeService employeeService;
    private final EmployeeSkillService skillService;
    private final StaffMapper staffMapper;

    @GetMapping
    List<EmployeeSummaryResponse> listActive() {
        return staffMapper.toSummaries(employeeService.list(EmploymentStatus.ACTIVE));
    }

    @GetMapping("/{id}")
    EmployeeSummaryResponse getById(@PathVariable Long id) {
        return staffMapper.toSummary(employeeService.getById(id));
    }

    @GetMapping("/{id}/skills")
    List<EmployeeSkillResponse> listSkills(@PathVariable Long id) {
        return staffMapper.toSkillResponses(skillService.listSkills(id));
    }
}
