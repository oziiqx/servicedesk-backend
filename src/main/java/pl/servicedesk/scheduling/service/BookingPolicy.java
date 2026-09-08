package pl.servicedesk.scheduling.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.SlotUnavailableException;
import pl.servicedesk.scheduling.BookingProperties;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.staff.domain.EmployeeWorkingHours;
import pl.servicedesk.staff.repository.EmployeeSkillRepository;
import pl.servicedesk.staff.repository.EmployeeTimeOffRepository;
import pl.servicedesk.staff.repository.EmployeeWorkingHoursRepository;

@Component
@RequiredArgsConstructor
class BookingPolicy {

    private final EmployeeWorkingHoursRepository workingHoursRepository;
    private final EmployeeTimeOffRepository timeOffRepository;
    private final EmployeeSkillRepository skillRepository;
    private final AppointmentRepository appointmentRepository;
    private final BookingProperties properties;
    private final Clock clock;

    void ensureBookableLeadTime(Instant start) {
        Instant now = clock.instant();
        if (start.isBefore(now.plus(properties.minLeadTime()))) {
            throw new BusinessRuleViolationException(
                    "Appointments must be booked at least %s in advance".formatted(properties.minLeadTime()));
        }
        Instant horizon = ZonedDateTime.ofInstant(now, properties.zoneId())
                .plus(properties.maxAdvanceBooking())
                .toInstant();
        if (start.isAfter(horizon)) {
            throw new BusinessRuleViolationException(
                    "Appointments cannot be booked more than %s ahead".formatted(properties.maxAdvanceBooking()));
        }
    }

    void ensureEmployeeCanPerform(Long employeeId, Collection<String> serviceCodes) {
        List<String> missing = serviceCodes.stream()
                .filter(code -> !skillRepository.existsByEmployeeIdAndServiceOfferingCode(employeeId, code))
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "The selected employee is not trained for: " + String.join(", ", missing));
        }
    }

    void ensureWithinWorkingHours(Long employeeId, Instant start, Instant end) {
        ZoneId zone = properties.zoneId();
        ZonedDateTime startLocal = start.atZone(zone);
        ZonedDateTime endLocal = end.atZone(zone);

        if (!startLocal.toLocalDate().equals(endLocal.toLocalDate())) {
            throw new BusinessRuleViolationException("An appointment must fall within a single day");
        }

        DayOfWeek day = startLocal.getDayOfWeek();
        boolean covered = workingHoursRepository
                .findByEmployeeIdOrderByDayOfWeekAscStartTimeAsc(employeeId).stream()
                .filter(hours -> hours.getDayOfWeek() == day)
                .anyMatch(hours -> fitsWithin(hours, startLocal, endLocal));

        if (!covered) {
            throw new BusinessRuleViolationException("The requested time is outside the employee's working hours");
        }
    }

    void ensureNotOnTimeOff(Long employeeId, Instant start, Instant end) {
        if (timeOffRepository.existsByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(employeeId, end, start)) {
            throw new BusinessRuleViolationException("The employee is on leave during the requested time");
        }
    }

    void ensureEmployeeSlotIsFree(Long employeeId, Instant start, Instant end, Long excludeAppointmentId) {
        if (appointmentRepository.existsEmployeeOverlap(
                employeeId, start, end, AppointmentStatus.slotBlockingStatuses(), excludeAppointmentId)) {
            throw new SlotUnavailableException("The employee already has an appointment in that time slot");
        }
    }

    void ensureResourceSlotIsFree(Long resourceId, Instant start, Instant end, Long excludeAppointmentId) {
        if (appointmentRepository.existsResourceOverlap(
                resourceId, start, end, AppointmentStatus.slotBlockingStatuses(), excludeAppointmentId)) {
            throw new SlotUnavailableException("The selected resource is busy in that time slot");
        }
    }

    private boolean fitsWithin(EmployeeWorkingHours hours, ZonedDateTime start, ZonedDateTime end) {
        return !start.toLocalTime().isBefore(hours.getStartTime())
                && !end.toLocalTime().isAfter(hours.getEndTime());
    }
}
