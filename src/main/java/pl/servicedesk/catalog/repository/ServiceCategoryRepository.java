package pl.servicedesk.catalog.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.servicedesk.catalog.domain.ServiceCategory;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<ServiceCategory> findAllByOrderByDisplayOrderAscNameAsc();
}
