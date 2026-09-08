package pl.servicedesk.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.common.error.InvalidAppointmentStateException;
import pl.servicedesk.scheduling.AppointmentCompleted;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class AppointmentLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant START = Instant.parse("2026-10-05T08:00:00Z");
    private static final Instant END = Instant.parse("2026-10-05T09:00:00Z");

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentPersister appointmentPersister;

    @Mock
    private BookingPolicy bookingPolicy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AppointmentLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new AppointmentLifecycleService(appointmentRepository, appointmentPersister,
                bookingPolicy, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void confirmsAPendingAppointment() {
        Appointment appointment = appointment(AppointmentStatus.PENDING);
        given(appointmentRepository.findWithDetailsById(1L)).willReturn(Optional.of(appointment));

        lifecycleService.changeStatus(1L, AppointmentStatus.CONFIRMED);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void completingAnAppointmentPublishesACompletionEvent() {
        Appointment appointment = appointment(AppointmentStatus.IN_PROGRESS);
        given(appointmentRepository.findWithDetailsById(1L)).willReturn(Optional.of(appointment));

        lifecycleService.changeStatus(1L, AppointmentStatus.COMPLETED);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(eventPublisher).publishEvent(new AppointmentCompleted(1L));
    }

    @Test
    void rejectsAnIllegalStatusJump() {
        Appointment appointment = appointment(AppointmentStatus.PENDING);
        given(appointmentRepository.findWithDetailsById(1L)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> lifecycleService.changeStatus(1L, AppointmentStatus.COMPLETED))
                .isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void cancelStampsReasonActorAndMoment() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
        given(appointmentRepository.findWithDetailsById(1L)).willReturn(Optional.of(appointment));

        lifecycleService.cancel(1L, 99L, "  changed my mind  ");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getCancellationReason()).isEqualTo("changed my mind");
        assertThat(appointment.getCancelledByUserId()).isEqualTo(99L);
        assertThat(appointment.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void rescheduleRevalidatesTheNewSlotAndPersists() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED);
        given(appointmentRepository.findWithDetailsById(1L)).willReturn(Optional.of(appointment));
        Instant newStart = Instant.parse("2026-10-06T10:00:00Z");

        lifecycleService.reschedule(1L, newStart);

        assertThat(appointment.getScheduledStart()).isEqualTo(newStart);
        assertThat(appointment.getScheduledEnd()).isEqualTo(newStart.plusSeconds(3600));
        verify(bookingPolicy).ensureWithinWorkingHours(eq(20L), eq(newStart), any());
        verify(bookingPolicy).ensureEmployeeSlotIsFree(20L, newStart, newStart.plusSeconds(3600), 1L);
        verify(appointmentPersister).persist(appointment);
    }

    @Test
    void rescheduleIsRejectedForACompletedAppointment() {
        Appointment appointment = appointment(AppointmentStatus.COMPLETED);
        given(appointmentRepository.findWithDetailsById(1L)).willReturn(Optional.of(appointment));

        assertThatThrownBy(() -> lifecycleService.reschedule(1L, Instant.parse("2026-10-06T10:00:00Z")))
                .isInstanceOf(InvalidAppointmentStateException.class);
    }

    private static Appointment appointment(AppointmentStatus status) {
        Client client = new Client(User.activeUser("c@example.com", "h", "Jan", "Kowalski", null), false);
        Employee employee = TestEntities.withId(new Employee(
                User.activeUser("e@example.com", "h", "Ola", "Nowak", null), "Dr Nowak", "Dentist",
                LocalDate.of(2024, 1, 1)), 20L);

        Appointment appointment = Appointment.schedule(client, employee, null, START, END);
        for (AppointmentStatus step : path(status)) {
            appointment.transitionTo(step);
        }
        return TestEntities.withId(appointment, 1L);
    }

    private static AppointmentStatus[] path(AppointmentStatus target) {
        return switch (target) {
            case PENDING -> new AppointmentStatus[] {};
            case CONFIRMED -> new AppointmentStatus[] {AppointmentStatus.CONFIRMED};
            case IN_PROGRESS -> new AppointmentStatus[] {AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS};
            case COMPLETED -> new AppointmentStatus[] {
                    AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED};
            default -> throw new IllegalArgumentException(target.name());
        };
    }
}
