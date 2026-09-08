package pl.servicedesk.billing.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.billing.domain.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByAppointmentId(Long appointmentId);

    @EntityGraph(attributePaths = {"appointment", "appointment.client", "appointment.client.user", "payments"})
    Optional<Invoice> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"appointment", "payments"})
    List<Invoice> findAllByOrderByIssuedAtDesc();

    @EntityGraph(attributePaths = {"appointment", "payments"})
    List<Invoice> findByAppointmentClientIdOrderByIssuedAtDesc(Long clientId);
}
