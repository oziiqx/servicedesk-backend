package pl.servicedesk.catalog.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.catalog.domain.ServiceOffering;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    boolean existsByCodeIgnoreCase(String code);

    List<ServiceOffering> findByCodeIn(Collection<String> codes);

    @EntityGraph(attributePaths = "category")
    Optional<ServiceOffering> findByCode(String code);

    @EntityGraph(attributePaths = "category")
    List<ServiceOffering> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "category")
    List<ServiceOffering> findAllByActiveTrueOrderByNameAsc();

    boolean existsByCategoryId(Long categoryId);
}
