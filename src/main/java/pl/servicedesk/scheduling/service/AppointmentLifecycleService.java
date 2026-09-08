package pl.servicedesk.scheduling.service;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;

@Service
@RequiredArgsConstructor
public class AppointmentLifecycleService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentPersister appointmentPersister;
    private final BookingPolicy bookingPolicy;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Appointment getById(Long appointmentId) {
        return appointmentRepository.findWithDetailsById(appointmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Appointment", appointmentId));
    }

    @Transactional
    public Appointment changeStatus(Long appointmentId, AppointmentStatus target) {
        Appointment appointment = getById(appointmentId);
        appointment.transitionTo(target);
        return appointment;
    }

    @Transactional
    public Appointment cancel(Long appointmentId, Long actingUserId, String reason) {
        Appointment appointment = getById(appointmentId);
        appointment.cancel(trimToNull(reason), actingUserId, clock.instant());
        return appointment;
    }

    @Transactional
    public Appointment reschedule(Long appointmentId, Instant newStart) {
        Appointment appointment = getById(appointmentId);
        Instant newEnd = newStart.plus(appointment.duration());
        Long employeeId = appointment.getEmployee().getId();

        bookingPolicy.ensureBookableLeadTime(newStart);
        bookingPolicy.ensureWithinWorkingHours(employeeId, newStart, newEnd);
        bookingPolicy.ensureNotOnTimeOff(employeeId, newStart, newEnd);
        bookingPolicy.ensureEmployeeSlotIsFree(employeeId, newStart, newEnd, appointment.getId());
        if (appointment.getResource() != null) {
            bookingPolicy.ensureResourceSlotIsFree(
                    appointment.getResource().getId(), newStart, newEnd, appointment.getId());
        }

        appointment.rescheduleTo(newStart, newEnd);
        appointmentPersister.persist(appointment);
        return appointment;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
