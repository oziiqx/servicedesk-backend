package pl.servicedesk.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.scheduling.domain.Appointment;

class InvoiceTest {

    private static final Instant NOW = Instant.parse("2026-05-10T09:00:00Z");

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoice = new Invoice(mock(Appointment.class), "INV-2026-000001", NOW, LocalDate.of(2026, 5, 24),
                new BigDecimal("100.00"), new BigDecimal("0.230"), new BigDecimal("23.00"), new BigDecimal("123.00"));
    }

    @Test
    void staysIssuedUntilItIsFullySettled() {
        invoice.recordPayment(new BigDecimal("50.00"), PaymentMethod.CARD, "auth-1", 1L, NOW);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.paidAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void becomesPaidOncePaymentsCoverTheGrossAmount() {
        invoice.recordPayment(new BigDecimal("100.00"), PaymentMethod.CARD, null, 1L, NOW);
        invoice.recordPayment(new BigDecimal("30.00"), PaymentMethod.CASH, null, 1L, NOW);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.paidAmount()).isEqualByComparingTo("130.00");
    }

    @Test
    void rejectsPaymentsOnACancelledInvoice() {
        invoice.cancel();

        assertThatThrownBy(() -> invoice.recordPayment(BigDecimal.TEN, PaymentMethod.CARD, null, 1L, NOW))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void doesNotAllowCancellingAPaidInvoice() {
        invoice.recordPayment(new BigDecimal("123.00"), PaymentMethod.TRANSFER, null, 1L, NOW);

        assertThatThrownBy(invoice::cancel)
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void marksOverdueOnlyAfterTheDueDate() {
        invoice.markOverdue(LocalDate.of(2026, 5, 24));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);

        invoice.markOverdue(LocalDate.of(2026, 5, 25));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }
}
