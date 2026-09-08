package pl.servicedesk.resources.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.resources.domain.ResourceType;

public interface BookableResourceRepository extends JpaRepository<BookableResource, Long> {

    boolean existsByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from BookableResource r where r.id = :id")
    Optional<BookableResource> findByIdForUpdate(Long id);

    List<BookableResource> findAllByOrderByNameAsc();

    List<BookableResource> findAllByActiveTrueOrderByNameAsc();

    List<BookableResource> findAllByTypeAndActiveTrue(ResourceType type);
}
