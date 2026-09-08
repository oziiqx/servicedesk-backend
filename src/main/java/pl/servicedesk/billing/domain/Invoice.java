package pl.servicedesk.billing.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.servicedesk.common.domain.AuditableEntity;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.scheduling.domain.Appointment;

@Entity
@Getter
@Table(name = "invoices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "tax_rate", nullable = false, precision = 4, scale = 3)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Payment> payments = new ArrayList<>();

    @Version
    private long version;

    public Invoice(Appointment appointment, String invoiceNumber, Instant issuedAt, LocalDate dueDate,
                   BigDecimal netAmount, BigDecimal taxRate, BigDecimal taxAmount, BigDecimal grossAmount) {
        this.appointment = appointment;
        this.invoiceNumber = invoiceNumber;
        this.issuedAt = issuedAt;
        this.dueDate = dueDate;
        this.netAmount = netAmount;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.grossAmount = grossAmount;
        this.status = InvoiceStatus.ISSUED;
    }

    public Payment recordPayment(BigDecimal amount, PaymentMethod method, String reference,
                                 Long recordedByUserId, Instant when) {
        if (status == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleViolationException("A cancelled invoice cannot receive payments");
        }
        Payment payment = new Payment(this, amount, method, reference, recordedByUserId, when);
        payments.add(payment);
        if (status != InvoiceStatus.PAID && isSettled()) {
            status = InvoiceStatus.PAID;
        }
        return payment;
    }

    public void markOverdue(LocalDate today) {
        if (status == InvoiceStatus.ISSUED && today.isAfter(dueDate)) {
            status = InvoiceStatus.OVERDUE;
        }
    }

    public void cancel() {
        if (status == InvoiceStatus.PAID) {
            throw new BusinessRuleViolationException("A paid invoice cannot be cancelled");
        }
        status = InvoiceStatus.CANCELLED;
    }

    public BigDecimal paidAmount() {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isSettled() {
        return paidAmount().compareTo(grossAmount) >= 0;
    }

    public List<Payment> getPayments() {
        return Collections.unmodifiableList(payments);
    }
}
