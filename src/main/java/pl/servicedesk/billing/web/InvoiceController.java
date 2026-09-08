package pl.servicedesk.billing.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.billing.service.InvoiceQueryService;
import pl.servicedesk.billing.service.PaymentService;
import pl.servicedesk.billing.web.dto.InvoiceResponse;
import pl.servicedesk.billing.web.dto.RecordPaymentRequest;
import pl.servicedesk.identity.security.AuthenticatedUser;
import pl.servicedesk.identity.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices")
class InvoiceController {

    private final InvoiceQueryService invoiceQueryService;
    private final PaymentService paymentService;
    private final BillingMapper billingMapper;

    @GetMapping
    List<InvoiceResponse> list(@CurrentUser AuthenticatedUser user) {
        return billingMapper.toResponses(invoiceQueryService.visibleTo(user));
    }

    @GetMapping("/{id}")
    InvoiceResponse getById(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return billingMapper.toResponse(invoiceQueryService.visibleToOrThrow(id, user));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasRole('ADMIN')")
    InvoiceResponse recordPayment(@CurrentUser AuthenticatedUser user, @PathVariable Long id,
                                  @Valid @RequestBody RecordPaymentRequest request) {
        return billingMapper.toResponse(paymentService.recordPayment(
                id, request.amount(), request.method(), request.reference(), user.id()));
    }
}
