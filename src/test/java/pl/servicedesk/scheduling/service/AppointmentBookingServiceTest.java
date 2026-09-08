package pl.servicedesk.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.catalog.domain.ServiceCategory;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.clients.repository.ClientRepository;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.common.error.SlotUnavailableException;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.pricing.AppointmentPricer;
import pl.servicedesk.pricing.AppointmentPricer.AppointmentPrice;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.resources.domain.ResourceType;
import pl.servicedesk.resources.repository.BookableResourceRepository;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.scheduling.service.AppointmentBookingService.BookCommand;
import pl.servicedesk.scheduling.service.AppointmentBookingService.ServiceLine;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmploymentStatus;
import pl.servicedesk.staff.repository.EmployeeRepository;
import pl.servicedesk.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class AppointmentBookingServiceTest {

    private static final Instant START = Instant.parse("2026-10-05T08:00:00Z");
    private static final Long CLIENT_USER_ID = 10L;
    private static final Long EMPLOYEE_ID = 20L;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ServiceOfferingRepository serviceRepository;

    @Mock
    private BookableResourceRepository resourceRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentPersister appointmentPersister;

    @Mock
    private BookingPolicy bookingPolicy;

    @Mock
    private AppointmentPricer appointmentPricer;

    @InjectMocks
    private AppointmentBookingService bookingService;

    private final List<Appointment> persisted = new ArrayList<>();

    @Test
    void booksAppointmentDerivingTheEndFromServiceDurations() {
        Client client = client();
        Employee employee = employee(EmploymentStatus.ACTIVE);
        ServiceOffering diagnostics = service("DIAG", new BigDecimal("120.00"), 30, 15, null);
        given(clientRepository.findByUserId(CLIENT_USER_ID)).willReturn(Optional.of(client));
        given(employeeRepository.findByIdForUpdate(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(serviceRepository.findByCodeIn(any())).willReturn(List.of(diagnostics));
        given(appointmentPricer.price(any(), any(), any())).willReturn(new AppointmentPrice(
                new BigDecimal("120.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("120.00"), null));
        recordPersistedAppointments();

        Appointment appointment = bookingService.book(new BookCommand(
                CLIENT_USER_ID, EMPLOYEE_ID, List.of(new ServiceLine("DIAG", 1)), START, null));

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getScheduledStart()).isEqualTo(START);
        assertThat(appointment.getScheduledEnd()).isEqualTo(START.plusSeconds(45 * 60));
        assertThat(appointment.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getService().getCode()).isEqualTo("DIAG"));
        assertThat(appointment.getTotalAmount()).isEqualByComparingTo("120.00");

        verify(bookingPolicy).ensureEmployeeCanPerform(EMPLOYEE_ID, List.of("DIAG"));
        verify(bookingPolicy).ensureWithinWorkingHours(EMPLOYEE_ID, START, START.plusSeconds(45 * 60));
        verify(bookingPolicy).ensureEmployeeSlotIsFree(EMPLOYEE_ID, START, START.plusSeconds(45 * 60), null);
        verify(appointmentPersister).persist(any(Appointment.class));
    }

    @Test
    void rejectsBookingWhenTheAccountIsNotAClient() {
        given(clientRepository.findByUserId(CLIENT_USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> book("DIAG"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsBookingWithAnUnknownEmployee() {
        given(clientRepository.findByUserId(CLIENT_USER_ID)).willReturn(Optional.of(client()));
        given(employeeRepository.findByIdForUpdate(EMPLOYEE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> book("DIAG"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsBookingWithASuspendedEmployee() {
        given(clientRepository.findByUserId(CLIENT_USER_ID)).willReturn(Optional.of(client()));
        given(employeeRepository.findByIdForUpdate(EMPLOYEE_ID))
                .willReturn(Optional.of(employee(EmploymentStatus.SUSPENDED)));

        assertThatThrownBy(() -> book("DIAG"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsBookingWithAnUnknownServiceCode() {
        given(clientRepository.findByUserId(CLIENT_USER_ID)).willReturn(Optional.of(client()));
        given(employeeRepository.findByIdForUpdate(EMPLOYEE_ID))
                .willReturn(Optional.of(employee(EmploymentStatus.ACTIVE)));
        given(serviceRepository.findByCodeIn(any())).willReturn(List.of());

        assertThatThrownBy(() -> book("DIAG"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsBookingAnInactiveService() {
        ServiceOffering inactive = service("DIAG", new BigDecimal("50.00"), 30, 0, null);
        inactive.deactivate();
        given(clientRepository.findByUserId(CLIENT_USER_ID)).willReturn(Optional.of(client()));
        given(employeeRepository.findByIdForUpdate(EMPLOYEE_ID))
                .willReturn(Optional.of(employee(EmploymentStatus.ACTIVE)));
        given(serviceRepository.findByCodeIn(any())).willReturn(List.of(inactive));

        assertThatThrownBy(() -> book("DIAG"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void propagatesSlotConflictRaisedByThePolicy() {
        given(clientRepository.findByUserId(CLIENT_USER_ID)).willReturn(Optional.of(client()));
        given(employeeRepository.findByIdForUpdate(EMPLOYEE_ID))
                .willReturn(Optional.of(employee(EmploymentStatus.ACTIVE)));
        given(serviceRepository.findByCodeIn(any()))
                .willReturn(List.of(service("DIAG", new BigDecimal("50.00"), 30, 0, null)));
        willThrow(new SlotUnavailableException("taken"))
                .given(bookingPolicy).ensureEmployeeSlotIsFree(anyLong(), any(), any(), any());

        assertThatThrownBy(() -> book("DIAG"))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void autoAssignsAFreeResourceWhenTheServiceRequiresOne() {
        Client client = client();
        Employee employee = employee(EmploymentStatus.ACTIVE);
        ServiceOffering massage = service("MASSAGE", new BigDecimal("200.00"), 60, 0, ResourceType.ROOM);
        BookableResource room = new BookableResource("Room 1", ResourceType.ROOM);
        given(clientRepository.findByUserId(CLIENT_USER_ID)).willReturn(Optional.of(client));
        given(employeeRepository.findByIdForUpdate(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(serviceRepository.findByCodeIn(any())).willReturn(List.of(massage));
        given(resourceRepository.findAllByTypeAndActiveTrue(ResourceType.ROOM)).willReturn(List.of(room));
        given(resourceRepository.findByIdForUpdate(any())).willReturn(Optional.of(room));
        given(appointmentRepository.existsResourceOverlap(any(), any(), any(), any(), any())).willReturn(false);
        given(appointmentPricer.price(any(), any(), any())).willReturn(new AppointmentPrice(
                new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("200.00"), null));
        recordPersistedAppointments();

        Appointment appointment = bookingService.book(new BookCommand(
                CLIENT_USER_ID, EMPLOYEE_ID, List.of(new ServiceLine("MASSAGE", 1)), START, null));

        assertThat(appointment.getResource()).isSameAs(room);
    }

    private void recordPersistedAppointments() {
        doAnswer(invocation -> {
            persisted.add(invocation.getArgument(0));
            return null;
        }).when(appointmentPersister).persist(any(Appointment.class));
        given(appointmentRepository.findWithDetailsById(any()))
                .willAnswer(invocation -> persisted.stream().reduce((first, second) -> second));
    }

    private Appointment book(String serviceCode) {
        return bookingService.book(new BookCommand(
                CLIENT_USER_ID, EMPLOYEE_ID, List.of(new ServiceLine(serviceCode, 1)), START, null));
    }

    private static Client client() {
        return new Client(User.activeUser("client@example.com", "h", "Jan", "Kowalski", null), false);
    }

    private static Employee employee(EmploymentStatus status) {
        Employee employee = new Employee(
                User.activeUser("emp@example.com", "h", "Ola", "Nowak", null), "Dr Nowak", "Dentist",
                LocalDate.of(2024, 1, 1));
        employee.changeStatus(status);
        return TestEntities.withId(employee, EMPLOYEE_ID);
    }

    private static ServiceOffering service(String code, BigDecimal price, int duration, int buffer,
                                           ResourceType resourceType) {
        return new ServiceOffering(new ServiceCategory("Category", null, 0), code, "Service " + code, null,
                price, duration, buffer, resourceType);
    }
}
