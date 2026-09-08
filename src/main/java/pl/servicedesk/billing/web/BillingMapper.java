package pl.servicedesk.billing.web;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.servicedesk.billing.domain.Invoice;
import pl.servicedesk.billing.domain.Payment;
import pl.servicedesk.billing.web.dto.InvoiceResponse;
import pl.servicedesk.billing.web.dto.PaymentResponse;

@Mapper
interface BillingMapper {

    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "paidAmount", expression = "java(invoice.paidAmount())")
    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponses(List<Invoice> invoices);

    PaymentResponse toPaymentResponse(Payment payment);
}
