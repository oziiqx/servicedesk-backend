package pl.servicedesk.scheduling.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.identity.security.AuthenticatedUser;
import pl.servicedesk.identity.security.CurrentUser;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.service.AppointmentBookingService;
import pl.servicedesk.scheduling.service.AppointmentBookingService.BookCommand;
import pl.servicedesk.scheduling.service.AppointmentBookingService.ServiceLine;
import pl.servicedesk.scheduling.service.AppointmentLifecycleService;
import pl.servicedesk.scheduling.service.AppointmentQueryService;
import pl.servicedesk.scheduling.web.dto.AppointmentResponse;
import pl.servicedesk.scheduling.web.dto.BookAppointmentRequest;
import pl.servicedesk.scheduling.web.dto.CancelAppointmentRequest;
import pl.servicedesk.scheduling.web.dto.RescheduleRequest;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments")
class AppointmentController {

    private final AppointmentBookingService bookingService;
    private final AppointmentLifecycleService lifecycleService;
    private final AppointmentQueryService queryService;
    private final AppointmentMapper appointmentMapper;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    AppointmentResponse book(@CurrentUser AuthenticatedUser user, @Valid @RequestBody BookAppointmentRequest request) {
        BookCommand command = new BookCommand(
                user.id(),
                request.employeeId(),
                request.services().stream()
                        .map(line -> new ServiceLine(line.serviceCode(), line.quantity()))
                        .toList(),
                request.scheduledStart(),
                request.resourceId());
        return appointmentMapper.toResponse(bookingService.book(command));
    }

    @GetMapping
    List<AppointmentResponse> list(@CurrentUser AuthenticatedUser user) {
        return appointmentMapper.toResponses(queryService.visibleTo(user));
    }

    @GetMapping("/{id}")
    AppointmentResponse getById(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return appointmentMapper.toResponse(queryService.visibleToOrThrow(id, user));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    AppointmentResponse reschedule(@CurrentUser AuthenticatedUser user, @PathVariable Long id,
                                   @Valid @RequestBody RescheduleRequest request) {
        requireClientOrAdmin(queryService.visibleToOrThrow(id, user), user, "reschedule");
        return appointmentMapper.toResponse(lifecycleService.reschedule(id, request.scheduledStart()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    AppointmentResponse cancel(@CurrentUser AuthenticatedUser user, @PathVariable Long id,
                               @RequestBody(required = false) @Valid CancelAppointmentRequest request) {
        requireClientOrAdmin(queryService.visibleToOrThrow(id, user), user, "cancel");
        String reason = request == null ? null : request.reason();
        return appointmentMapper.toResponse(lifecycleService.cancel(id, user.id(), reason));
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    AppointmentResponse transition(@CurrentUser AuthenticatedUser user, @PathVariable Long id,
                                   @RequestParam AppointmentStatus status) {
        Appointment appointment = queryService.visibleToOrThrow(id, user);
        if (!user.hasRole("ADMIN") && !queryService.isAssignedEmployee(appointment, user)) {
            throw new AccessDeniedException("Only the assigned employee or an admin can change the status");
        }
        return appointmentMapper.toResponse(lifecycleService.changeStatus(id, status));
    }

    private void requireClientOrAdmin(Appointment appointment, AuthenticatedUser user, String action) {
        if (!user.hasRole("ADMIN") && !queryService.isOwner(appointment, user)) {
            throw new AccessDeniedException("Only the booking client or an admin can " + action + " this appointment");
        }
    }
}
