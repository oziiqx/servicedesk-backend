package pl.servicedesk.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.catalog.domain.ServiceCategory;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.resources.repository.BookableResourceRepository;
import pl.servicedesk.scheduling.BookingProperties;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.scheduling.service.AvailabilityService.AvailableSlot;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeWorkingHours;
import pl.servicedesk.staff.repository.EmployeeRepository;
import pl.servicedesk.staff.repository.EmployeeSkillRepository;
import pl.servicedesk.staff.repository.EmployeeTimeOffRepository;
import pl.servicedesk.staff.repository.EmployeeWorkingHoursRepository;
import pl.servicedesk.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final long EMPLOYEE_ID = 20L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    @Mock private ServiceOfferingRepository serviceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeSkillRepository skillRepository;
    @Mock private EmployeeWorkingHoursRepository workingHoursRepository;
    @Mock private EmployeeTimeOffRepository timeOffRepository;
    @Mock private BookableResourceRepository resourceRepository;
    @Mock private AppointmentRepository appointmentRepository;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        BookingProperties properties = new BookingProperties(
                "Europe/Warsaw", Duration.ofHours(1), Period.ofDays(60), Duration.ofHours(24), Duration.ofMinutes(60));
        availabilityService = new AvailabilityService(serviceRepository, employeeRepository, skillRepository,
                workingHoursRepository, timeOffRepository, resourceRepository, appointmentRepository, properties,
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void offersOneSlotPerHourWithinTheWorkingBlock() {
        stubEmployeeAndService();
        given(appointmentRepository.findEmployeeAppointmentsOverlapping(eq(EMPLOYEE_ID), any(), any(), any()))
                .willReturn(List.of());

        List<AvailableSlot> slots = availabilityService.findOpenSlots(EMPLOYEE_ID, "MASSAGE", MONDAY);

        assertThat(slots).extracting(AvailableSlot::start).containsExactly(
                Instant.parse("2026-11-02T08:00:00Z"),
                Instant.parse("2026-11-02T09:00:00Z"),
                Instant.parse("2026-11-02T10:00:00Z"));
    }

    @Test
    void dropsSlotsThatCollideWithAnExistingAppointment() {
        stubEmployeeAndService();
        Appointment booked = Appointment.schedule(
                new Client(User.activeUser("c@example.com", "h", "Jan", "Kowalski", null), false),
                employee(), null,
                Instant.parse("2026-11-02T09:00:00Z"), Instant.parse("2026-11-02T10:00:00Z"));
        given(appointmentRepository.findEmployeeAppointmentsOverlapping(eq(EMPLOYEE_ID), any(), any(), any()))
                .willReturn(List.of(booked));

        List<AvailableSlot> slots = availabilityService.findOpenSlots(EMPLOYEE_ID, "MASSAGE", MONDAY);

        assertThat(slots).extracting(AvailableSlot::start).containsExactly(
                Instant.parse("2026-11-02T08:00:00Z"),
                Instant.parse("2026-11-02T10:00:00Z"));
    }

    private void stubEmployeeAndService() {
        given(serviceRepository.findByCode("MASSAGE")).willReturn(Optional.of(
                new ServiceOffering(new ServiceCategory("Wellness", null, 0), "MASSAGE", "Massage", null,
                        new BigDecimal("200.00"), 60, 0, null)));
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee()));
        given(skillRepository.existsByEmployeeIdAndServiceOfferingCode(EMPLOYEE_ID, "MASSAGE")).willReturn(true);
        given(workingHoursRepository.findByEmployeeIdOrderByDayOfWeekAscStartTimeAsc(EMPLOYEE_ID)).willReturn(List.of(
                new EmployeeWorkingHours(employee(), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));
        given(timeOffRepository.findByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(eq(EMPLOYEE_ID), any(), any()))
                .willReturn(List.of());
    }

    private static Employee employee() {
        return TestEntities.withId(new Employee(
                User.activeUser("e@example.com", "h", "Ola", "Nowak", null), "Dr Nowak", "Masseuse",
                LocalDate.of(2024, 1, 1)), EMPLOYEE_ID);
    }
}
