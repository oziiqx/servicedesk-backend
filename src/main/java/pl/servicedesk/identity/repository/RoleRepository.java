package pl.servicedesk.identity.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
