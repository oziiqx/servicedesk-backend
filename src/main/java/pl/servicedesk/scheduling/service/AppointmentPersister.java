package pl.servicedesk.scheduling.service;

import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import pl.servicedesk.common.error.SlotUnavailableException;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.repository.AppointmentRepository;

@Component
@RequiredArgsConstructor
class AppointmentPersister {

    private static final String EXCLUSION_VIOLATION = "23P01";

    private final AppointmentRepository appointmentRepository;

    void persist(Appointment appointment) {
        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            if (isSlotConflict(exception)) {
                throw new SlotUnavailableException("That time slot was just taken by another booking");
            }
            throw exception;
        }
    }

    private boolean isSlotConflict(DataIntegrityViolationException exception) {
        return exception.getMostSpecificCause() instanceof SQLException sqlException
                && EXCLUSION_VIOLATION.equals(sqlException.getSQLState());
    }
}
