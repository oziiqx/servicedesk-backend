package pl.servicedesk.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.billing.BillingProperties;
import pl.servicedesk.billing.domain.Invoice;
import pl.servicedesk.billing.domain.InvoiceStatus;
import pl.servicedesk.billing.repository.InvoiceRepository;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class InvoiceIssuerTest {

    private static final Instant NOW = Instant.parse("2026-05-10T09:00:00Z");

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceNumberSequence invoiceNumberSequence;

    private InvoiceIssuer invoiceIssuer;

    @BeforeEach
    void setUp() {
        invoiceIssuer = new InvoiceIssuer(appointmentRepository, invoiceRepository, invoiceNumberSequence,
                new BillingProperties(new BigDecimal("0.230"), 14), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issuesAnInvoiceWithTaxAddedOnTopOfTheAppointmentTotal() {
        given(appointmentRepository.findById(5L)).willReturn(Optional.of(completedAppointment("200.00")));
        given(invoiceRepository.existsByAppointmentId(5L)).willReturn(false);
        given(invoiceNumberSequence.next()).willReturn("INV-2026-000001");
        given(invoiceRepository.save(any(Invoice.class))).willAnswer(call -> call.getArgument(0));

        invoiceIssuer.issueFor(5L);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice invoice = captor.getValue();
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-2026-000001");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.getNetAmount()).isEqualByComparingTo("200.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("46.00");
        assertThat(invoice.getGrossAmount()).isEqualByComparingTo("246.00");
        assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2026, 5, 24));
    }

    @Test
    void refusesToInvoiceAnAppointmentThatIsNotComplete() {
        Appointment pending = appointment("100.00");
        given(appointmentRepository.findById(5L)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> invoiceIssuer.issueFor(5L))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void refusesToInvoiceTheSameAppointmentTwice() {
        given(appointmentRepository.findById(5L)).willReturn(Optional.of(completedAppointment("100.00")));
        given(invoiceRepository.existsByAppointmentId(5L)).willReturn(true);

        assertThatThrownBy(() -> invoiceIssuer.issueFor(5L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(invoiceRepository, never()).save(any());
    }

    private static Appointment completedAppointment(String total) {
        Appointment appointment = appointment(total);
        appointment.transitionTo(AppointmentStatus.CONFIRMED);
        appointment.transitionTo(AppointmentStatus.IN_PROGRESS);
        appointment.transitionTo(AppointmentStatus.COMPLETED);
        return appointment;
    }

    private static Appointment appointment(String total) {
        Client client = new Client(User.activeUser("c@example.com", "h", "A", "B", null), false);
        Employee employee = TestEntities.withId(new Employee(
                User.activeUser("e@example.com", "h", "O", "N", null), "Dr N", "Dentist",
                LocalDate.of(2024, 1, 1)), 20L);
        Appointment appointment = Appointment.schedule(client, employee, null,
                Instant.parse("2026-05-09T08:00:00Z"), Instant.parse("2026-05-09T09:00:00Z"));
        appointment.applyPricing(new BigDecimal(total), new BigDecimal("0.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal(total), "BRONZE");
        return TestEntities.withId(appointment, 5L);
    }
}
