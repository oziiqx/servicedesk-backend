package pl.servicedesk.billing.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        String status,
        Long appointmentId,
        Instant issuedAt,
        LocalDate dueDate,
        BigDecimal netAmount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal grossAmount,
        BigDecimal paidAmount,
        List<PaymentResponse> payments) {
}
