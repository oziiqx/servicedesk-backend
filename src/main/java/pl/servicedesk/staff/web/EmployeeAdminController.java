package pl.servicedesk.staff.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.staff.domain.EmploymentStatus;
import pl.servicedesk.staff.service.EmployeeService;
import pl.servicedesk.staff.service.EmployeeService.OnboardCommand;
import pl.servicedesk.staff.service.EmployeeSkillService;
import pl.servicedesk.staff.web.dto.EmployeeResponse;
import pl.servicedesk.staff.web.dto.EmployeeSkillResponse;
import pl.servicedesk.staff.web.dto.OnboardEmployeeRequest;
import pl.servicedesk.staff.web.dto.SkillAssignmentRequest;
import pl.servicedesk.staff.web.dto.UpdateEmployeeRequest;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Staff administration")
class EmployeeAdminController {

    private final EmployeeService employeeService;
    private final EmployeeSkillService skillService;
    private final StaffMapper staffMapper;

    @PostMapping
    ResponseEntity<EmployeeResponse> onboard(@Valid @RequestBody OnboardEmployeeRequest request) {
        EmployeeResponse created = staffMapper.toResponse(employeeService.onboard(new OnboardCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.displayName(),
                request.title(),
                request.hiredOn())));
        return ResponseEntity.created(URI.create("/api/v1/admin/employees/" + created.id())).body(created);
    }

    @GetMapping
    List<EmployeeResponse> list(@RequestParam(required = false) EmploymentStatus status) {
        return staffMapper.toResponses(employeeService.list(status));
    }

    @GetMapping("/{id}")
    EmployeeResponse getById(@PathVariable Long id) {
        return staffMapper.toResponse(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return staffMapper.toResponse(employeeService.updateProfile(id, request.displayName(), request.title()));
    }

    @PostMapping("/{id}/employment-status")
    EmployeeResponse changeStatus(@PathVariable Long id, @RequestParam EmploymentStatus status) {
        return staffMapper.toResponse(employeeService.changeStatus(id, status));
    }

    @GetMapping("/{id}/skills")
    List<EmployeeSkillResponse> listSkills(@PathVariable Long id) {
        return staffMapper.toSkillResponses(skillService.listSkills(id));
    }

    @PutMapping("/{id}/skills")
    List<EmployeeSkillResponse> replaceSkills(@PathVariable Long id, @Valid @RequestBody SkillAssignmentRequest request) {
        return staffMapper.toSkillResponses(skillService.replaceSkills(id, request.serviceCodes()));
    }
}
