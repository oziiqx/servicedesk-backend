package pl.servicedesk.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.billing.BillingProperties;
import pl.servicedesk.billing.domain.Invoice;
import pl.servicedesk.billing.repository.InvoiceRepository;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;

@Service
@RequiredArgsConstructor
public class InvoiceIssuer {

    private final AppointmentRepository appointmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberSequence invoiceNumberSequence;
    private final BillingProperties billingProperties;
    private final Clock clock;

    @Transactional
    public Invoice issueFor(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Appointment", appointmentId));
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessRuleViolationException("Only completed appointments can be invoiced");
        }
        if (invoiceRepository.existsByAppointmentId(appointmentId)) {
            throw new DuplicateResourceException("Appointment %d is already invoiced".formatted(appointmentId));
        }

        BigDecimal net = appointment.getTotalAmount();
        BigDecimal taxRate = billingProperties.invoiceTaxRate();
        BigDecimal taxAmount = net.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = net.add(taxAmount);
        Instant now = clock.instant();
        LocalDate dueDate = LocalDate.ofInstant(now, clock.getZone())
                .plusDays(billingProperties.invoicePaymentTermDays());

        return invoiceRepository.save(new Invoice(
                appointment, invoiceNumberSequence.next(), now, dueDate, net, taxRate, taxAmount, gross));
    }
}
