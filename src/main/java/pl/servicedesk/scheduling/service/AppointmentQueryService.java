package pl.servicedesk.scheduling.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.clients.repository.ClientRepository;
import pl.servicedesk.identity.security.AuthenticatedUser;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.staff.repository.EmployeeRepository;

@Service
@RequiredArgsConstructor
public class AppointmentQueryService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<Appointment> visibleTo(AuthenticatedUser user) {
        if (user.hasRole("ADMIN")) {
            return appointmentRepository.findAllByOrderByScheduledStartDesc();
        }
        if (user.hasRole("EMPLOYEE")) {
            return employeeRepository.findWithUserByUserId(user.id())
                    .map(employee -> appointmentRepository.findByEmployeeIdOrderByScheduledStartAsc(employee.getId()))
                    .orElseGet(List::of);
        }
        return clientRepository.findByUserId(user.id())
                .map(client -> appointmentRepository.findByClientIdOrderByScheduledStartDesc(client.getId()))
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public Appointment visibleToOrThrow(Long appointmentId, AuthenticatedUser user) {
        Appointment appointment = appointmentRepository.findWithDetailsById(appointmentId)
                .orElseThrow(() -> new AccessDeniedException("You cannot access this appointment"));
        if (canView(appointment, user)) {
            return appointment;
        }
        throw new AccessDeniedException("You cannot access this appointment");
    }

    public boolean canView(Appointment appointment, AuthenticatedUser user) {
        return user.hasRole("ADMIN")
                || isOwner(appointment, user)
                || isAssignedEmployee(appointment, user);
    }

    public boolean isOwner(Appointment appointment, AuthenticatedUser user) {
        return appointment.getClient().getUser().getId().equals(user.id());
    }

    public boolean isAssignedEmployee(Appointment appointment, AuthenticatedUser user) {
        return appointment.getEmployee().getUser().getId().equals(user.id());
    }
}
