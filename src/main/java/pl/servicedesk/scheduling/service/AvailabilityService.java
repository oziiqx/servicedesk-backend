package pl.servicedesk.scheduling.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.resources.repository.BookableResourceRepository;
import pl.servicedesk.scheduling.BookingProperties;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeTimeOff;
import pl.servicedesk.staff.domain.EmployeeWorkingHours;
import pl.servicedesk.staff.repository.EmployeeRepository;
import pl.servicedesk.staff.repository.EmployeeSkillRepository;
import pl.servicedesk.staff.repository.EmployeeTimeOffRepository;
import pl.servicedesk.staff.repository.EmployeeWorkingHoursRepository;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ServiceOfferingRepository serviceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository skillRepository;
    private final EmployeeWorkingHoursRepository workingHoursRepository;
    private final EmployeeTimeOffRepository timeOffRepository;
    private final BookableResourceRepository resourceRepository;
    private final AppointmentRepository appointmentRepository;
    private final BookingProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AvailableSlot> findOpenSlots(Long employeeId, String serviceCode, LocalDate date) {
        ServiceOffering service = serviceRepository.findByCode(serviceCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Service", serviceCode));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
        if (!employee.isBookable()) {
            return List.of();
        }
        if (!skillRepository.existsByEmployeeIdAndServiceOfferingCode(employeeId, serviceCode)) {
            throw new BusinessRuleViolationException("The employee is not trained for service " + serviceCode);
        }

        ZoneId zone = properties.zoneId();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        Duration slotLength = service.slotLength();
        Instant earliestStart = clock.instant().plus(properties.minLeadTime());

        var blocking = AppointmentStatus.slotBlockingStatuses();
        List<Appointment> dayAppointments = appointmentRepository
                .findEmployeeAppointmentsOverlapping(employeeId, dayStart, dayEnd, blocking);
        List<EmployeeTimeOff> dayTimeOff = timeOffRepository
                .findByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(employeeId, dayEnd, dayStart);
        List<BookableResource> resources = service.getRequiredResourceType() == null
                ? List.of()
                : resourceRepository.findAllByTypeAndActiveTrue(service.getRequiredResourceType());
        List<Appointment> resourceAppointments = resources.isEmpty()
                ? List.of()
                : appointmentRepository.findResourceAppointmentsOverlapping(
                        resources.stream().map(BookableResource::getId).toList(), dayStart, dayEnd, blocking);

        List<AvailableSlot> slots = new ArrayList<>();
        for (EmployeeWorkingHours block : workingHoursRepository
                .findByEmployeeIdOrderByDayOfWeekAscStartTimeAsc(employeeId)) {
            if (block.getDayOfWeek() != date.getDayOfWeek()) {
                continue;
            }
            for (LocalTime cursor = block.getStartTime();
                 !cursor.plus(slotLength).isAfter(block.getEndTime());
                 cursor = cursor.plus(properties.slotGranularity())) {

                ZonedDateTime slotStartLocal = ZonedDateTime.of(date, cursor, zone);
                Instant slotStart = slotStartLocal.toInstant();
                Instant slotEnd = slotStartLocal.plus(slotLength).toInstant();

                if (slotStart.isBefore(earliestStart)) {
                    continue;
                }
                if (overlapsAny(dayAppointments, slotStart, slotEnd)) {
                    continue;
                }
                if (dayTimeOff.stream().anyMatch(off -> off.overlaps(slotStart, slotEnd))) {
                    continue;
                }
                if (!resources.isEmpty() && !anyResourceFree(resources, resourceAppointments, slotStart, slotEnd)) {
                    continue;
                }
                slots.add(new AvailableSlot(slotStart, slotEnd));
            }
        }
        return slots;
    }

    private boolean overlapsAny(List<Appointment> appointments, Instant start, Instant end) {
        return appointments.stream()
                .anyMatch(a -> a.getScheduledStart().isBefore(end) && a.getScheduledEnd().isAfter(start));
    }

    private boolean anyResourceFree(List<BookableResource> resources, List<Appointment> resourceAppointments,
                                    Instant start, Instant end) {
        return resources.stream().anyMatch(resource -> resourceAppointments.stream()
                .filter(a -> a.getResource() != null && a.getResource().getId().equals(resource.getId()))
                .noneMatch(a -> a.getScheduledStart().isBefore(end) && a.getScheduledEnd().isAfter(start)));
    }

    public record AvailableSlot(Instant start, Instant end) {
    }
}
