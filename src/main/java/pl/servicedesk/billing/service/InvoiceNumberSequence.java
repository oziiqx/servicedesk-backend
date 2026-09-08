package pl.servicedesk.billing.service;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class InvoiceNumberSequence {

    private final EntityManager entityManager;
    private final Clock clock;

    InvoiceNumberSequence(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public String next() {
        Number value = (Number) entityManager
                .createNativeQuery("select nextval('invoice_number_seq')")
                .getSingleResult();
        return "INV-%d-%06d".formatted(LocalDate.now(clock).getYear(), value.longValue());
    }
}
