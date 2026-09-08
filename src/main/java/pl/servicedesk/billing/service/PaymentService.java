package pl.servicedesk.billing.service;

import java.math.BigDecimal;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.billing.domain.Invoice;
import pl.servicedesk.billing.domain.PaymentMethod;
import pl.servicedesk.billing.repository.InvoiceRepository;
import pl.servicedesk.common.error.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    @Transactional
    public Invoice recordPayment(Long invoiceId, BigDecimal amount, PaymentMethod method,
                                 String reference, Long recordedByUserId) {
        Invoice invoice = invoiceRepository.findWithDetailsById(invoiceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Invoice", invoiceId));
        invoice.recordPayment(amount, method, trimToNull(reference), recordedByUserId, clock.instant());
        return invoice;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
