package pl.servicedesk.clients.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.clients.domain.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<Client> findWithUserByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
