package pl.servicedesk.staff.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeTimeOff;
import pl.servicedesk.staff.repository.EmployeeTimeOffRepository;

@Service
@RequiredArgsConstructor
public class TimeOffService {

    private final EmployeeTimeOffRepository timeOffRepository;
    private final EmployeeService employeeService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EmployeeTimeOff> listUpcoming(Long employeeId) {
        return timeOffRepository.findByEmployeeIdAndEndsAtAfterOrderByStartsAtAsc(employeeId, clock.instant());
    }

    @Transactional
    public EmployeeTimeOff request(Long employeeId, Instant startsAt, Instant endsAt, String reason) {
        Instant now = clock.instant();
        if (!startsAt.isBefore(endsAt)) {
            throw new BusinessRuleViolationException("Time off must start before it ends");
        }
        if (startsAt.isBefore(now)) {
            throw new BusinessRuleViolationException("Time off cannot start in the past");
        }
        if (timeOffRepository.existsByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(employeeId, endsAt, startsAt)) {
            throw new BusinessRuleViolationException("Time off overlaps an existing entry");
        }

        Employee employee = employeeService.getById(employeeId);
        return timeOffRepository.save(new EmployeeTimeOff(employee, startsAt, endsAt, blankToNull(reason)));
    }

    @Transactional
    public void cancel(Long employeeId, Long timeOffId) {
        EmployeeTimeOff timeOff = timeOffRepository.findByIdAndEmployeeId(timeOffId, employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Time off", timeOffId));
        timeOffRepository.delete(timeOff);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
