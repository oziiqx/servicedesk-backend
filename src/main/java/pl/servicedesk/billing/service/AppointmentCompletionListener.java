package pl.servicedesk.billing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.servicedesk.scheduling.AppointmentCompleted;

@Component
@RequiredArgsConstructor
class AppointmentCompletionListener {

    private final InvoiceIssuer invoiceIssuer;

    @EventListener
    void onAppointmentCompleted(AppointmentCompleted event) {
        invoiceIssuer.issueFor(event.appointmentId());
    }
}
