package pl.servicedesk.staff.web;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeSkill;
import pl.servicedesk.staff.domain.EmployeeTimeOff;
import pl.servicedesk.staff.domain.EmployeeWorkingHours;
import pl.servicedesk.staff.web.dto.EmployeeResponse;
import pl.servicedesk.staff.web.dto.EmployeeSkillResponse;
import pl.servicedesk.staff.web.dto.EmployeeSummaryResponse;
import pl.servicedesk.staff.web.dto.TimeOffResponse;
import pl.servicedesk.staff.web.dto.WorkingHoursResponse;

@Mapper
interface StaffMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "fullName", source = "user")
    EmployeeResponse toResponse(Employee employee);

    List<EmployeeResponse> toResponses(List<Employee> employees);

    EmployeeSummaryResponse toSummary(Employee employee);

    List<EmployeeSummaryResponse> toSummaries(List<Employee> employees);

    WorkingHoursResponse toResponse(EmployeeWorkingHours workingHours);

    List<WorkingHoursResponse> toWorkingHoursResponses(List<EmployeeWorkingHours> workingHours);

    TimeOffResponse toResponse(EmployeeTimeOff timeOff);

    List<TimeOffResponse> toTimeOffResponses(List<EmployeeTimeOff> timeOff);

    @Mapping(target = "serviceCode", source = "serviceOffering.code")
    @Mapping(target = "serviceName", source = "serviceOffering.name")
    EmployeeSkillResponse toResponse(EmployeeSkill skill);

    List<EmployeeSkillResponse> toSkillResponses(List<EmployeeSkill> skills);

    default String userFullName(User user) {
        return user.fullName();
    }
}
