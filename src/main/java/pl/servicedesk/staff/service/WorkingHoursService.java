package pl.servicedesk.staff.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeWorkingHours;
import pl.servicedesk.staff.repository.EmployeeWorkingHoursRepository;

@Service
@RequiredArgsConstructor
public class WorkingHoursService {

    private final EmployeeWorkingHoursRepository workingHoursRepository;
    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public List<EmployeeWorkingHours> getSchedule(Long employeeId) {
        return workingHoursRepository.findByEmployeeIdOrderByDayOfWeekAscStartTimeAsc(employeeId);
    }

    @Transactional
    public List<EmployeeWorkingHours> replaceSchedule(Long employeeId, List<DayShift> shifts) {
        rejectInvalidShifts(shifts);
        Employee employee = employeeService.getById(employeeId);

        workingHoursRepository.deleteByEmployeeId(employeeId);
        workingHoursRepository.flush();

        return workingHoursRepository.saveAll(shifts.stream()
                .map(shift -> new EmployeeWorkingHours(employee, shift.dayOfWeek(), shift.startTime(), shift.endTime()))
                .toList());
    }

    private void rejectInvalidShifts(List<DayShift> shifts) {
        EnumSet<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
        for (DayShift shift : shifts) {
            if (!shift.startTime().isBefore(shift.endTime())) {
                throw new BusinessRuleViolationException(
                        "%s working hours must start before they end".formatted(shift.dayOfWeek()));
            }
            if (!seen.add(shift.dayOfWeek())) {
                throw new BusinessRuleViolationException("Duplicate working hours for " + shift.dayOfWeek());
            }
        }
    }

    public record DayShift(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
