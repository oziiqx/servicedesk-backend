package pl.servicedesk.resources.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.resources.domain.ResourceType;

public interface BookableResourceRepository extends JpaRepository<BookableResource, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<BookableResource> findAllByOrderByNameAsc();

    List<BookableResource> findAllByActiveTrueOrderByNameAsc();

    List<BookableResource> findAllByTypeAndActiveTrue(ResourceType type);
}
