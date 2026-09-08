package pl.servicedesk.staff.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.identity.security.AuthenticatedUser;
import pl.servicedesk.identity.security.CurrentUser;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.service.EmployeeService;
import pl.servicedesk.staff.service.TimeOffService;
import pl.servicedesk.staff.service.WorkingHoursService;
import pl.servicedesk.staff.service.WorkingHoursService.DayShift;
import pl.servicedesk.staff.web.dto.EmployeeResponse;
import pl.servicedesk.staff.web.dto.TimeOffRequest;
import pl.servicedesk.staff.web.dto.TimeOffResponse;
import pl.servicedesk.staff.web.dto.WeeklyScheduleRequest;
import pl.servicedesk.staff.web.dto.WorkingHoursResponse;

@RestController
@RequestMapping("/api/v1/employees/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
@Tag(name = "My schedule")
class MyScheduleController {

    private final EmployeeService employeeService;
    private final WorkingHoursService workingHoursService;
    private final TimeOffService timeOffService;
    private final StaffMapper staffMapper;

    @GetMapping
    EmployeeResponse myProfile(@CurrentUser AuthenticatedUser currentUser) {
        return staffMapper.toResponse(currentEmployee(currentUser));
    }

    @GetMapping("/working-hours")
    List<WorkingHoursResponse> myWorkingHours(@CurrentUser AuthenticatedUser currentUser) {
        return staffMapper.toWorkingHoursResponses(
                workingHoursService.getSchedule(currentEmployee(currentUser).getId()));
    }

    @PutMapping("/working-hours")
    List<WorkingHoursResponse> replaceWorkingHours(@CurrentUser AuthenticatedUser currentUser,
                                                   @Valid @RequestBody WeeklyScheduleRequest request) {
        List<DayShift> shifts = request.shifts().stream()
                .map(shift -> new DayShift(shift.dayOfWeek(), shift.startTime(), shift.endTime()))
                .toList();
        return staffMapper.toWorkingHoursResponses(
                workingHoursService.replaceSchedule(currentEmployee(currentUser).getId(), shifts));
    }

    @GetMapping("/time-off")
    List<TimeOffResponse> myTimeOff(@CurrentUser AuthenticatedUser currentUser) {
        return staffMapper.toTimeOffResponses(
                timeOffService.listUpcoming(currentEmployee(currentUser).getId()));
    }

    @PostMapping("/time-off")
    @ResponseStatus(HttpStatus.CREATED)
    TimeOffResponse requestTimeOff(@CurrentUser AuthenticatedUser currentUser,
                                   @Valid @RequestBody TimeOffRequest request) {
        return staffMapper.toResponse(timeOffService.request(
                currentEmployee(currentUser).getId(), request.startsAt(), request.endsAt(), request.reason()));
    }

    @DeleteMapping("/time-off/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelTimeOff(@CurrentUser AuthenticatedUser currentUser, @PathVariable Long id) {
        timeOffService.cancel(currentEmployee(currentUser).getId(), id);
    }

    private Employee currentEmployee(AuthenticatedUser currentUser) {
        return employeeService.requireByUserId(currentUser.id());
    }
}
