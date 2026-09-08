package pl.servicedesk.billing.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.billing.domain.Invoice;
import pl.servicedesk.billing.repository.InvoiceRepository;
import pl.servicedesk.clients.repository.ClientRepository;
import pl.servicedesk.identity.security.AuthenticatedUser;

@Service
@RequiredArgsConstructor
public class InvoiceQueryService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<Invoice> visibleTo(AuthenticatedUser user) {
        if (user.hasRole("ADMIN")) {
            return invoiceRepository.findAllByOrderByIssuedAtDesc();
        }
        return clientRepository.findByUserId(user.id())
                .map(client -> invoiceRepository.findByAppointmentClientIdOrderByIssuedAtDesc(client.getId()))
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public Invoice visibleToOrThrow(Long invoiceId, AuthenticatedUser user) {
        Invoice invoice = invoiceRepository.findWithDetailsById(invoiceId)
                .orElseThrow(() -> new AccessDeniedException("You cannot access this invoice"));
        boolean owner = invoice.getAppointment().getClient().getUser().getId().equals(user.id());
        if (user.hasRole("ADMIN") || owner) {
            return invoice;
        }
        throw new AccessDeniedException("You cannot access this invoice");
    }
}
