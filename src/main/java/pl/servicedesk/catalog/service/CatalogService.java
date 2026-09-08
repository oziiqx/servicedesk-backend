package pl.servicedesk.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.catalog.domain.ServiceCategory;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.repository.ServiceCategoryRepository;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.resources.domain.ResourceType;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ServiceOfferingRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ServiceCategory> listCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<ServiceOffering> listServices(boolean includeInactive) {
        return includeInactive
                ? serviceRepository.findAllByOrderByNameAsc()
                : serviceRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public ServiceOffering getServiceByCode(String code) {
        return serviceRepository.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Service", code));
    }

    @Transactional
    public ServiceCategory createCategory(String name, String description, int displayOrder) {
        String normalized = name.trim();
        if (categoryRepository.existsByNameIgnoreCase(normalized)) {
            throw new DuplicateResourceException("Category '%s' already exists".formatted(normalized));
        }
        return categoryRepository.save(new ServiceCategory(normalized, trimToNull(description), displayOrder));
    }

    @Transactional
    public ServiceCategory updateCategory(Long id, String name, String description, int displayOrder) {
        String normalized = name.trim();
        ServiceCategory category = requireCategory(id);
        if (!category.getName().equalsIgnoreCase(normalized) && categoryRepository.existsByNameIgnoreCase(normalized)) {
            throw new DuplicateResourceException("Category '%s' already exists".formatted(normalized));
        }
        category.rename(normalized);
        category.describe(trimToNull(description));
        category.reorder(displayOrder);
        return category;
    }

    @Transactional
    public ServiceOffering createService(NewService data) {
        String code = data.code().trim();
        if (serviceRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Service code '%s' is already in use".formatted(code));
        }
        ServiceCategory category = requireCategory(data.categoryId());
        ServiceOffering service = new ServiceOffering(
                category,
                code,
                data.name().trim(),
                trimToNull(data.description()),
                data.basePrice(),
                data.durationMinutes(),
                data.bufferMinutes(),
                data.requiredResourceType());
        return serviceRepository.save(service);
    }

    @Transactional
    public ServiceOffering updateService(String code, ServiceUpdate data) {
        ServiceOffering service = getServiceByCode(code);
        if (!service.getCategory().getId().equals(data.categoryId())) {
            service.moveToCategory(requireCategory(data.categoryId()));
        }
        service.updateDetails(
                data.name().trim(),
                trimToNull(data.description()),
                data.durationMinutes(),
                data.bufferMinutes(),
                data.requiredResourceType());
        service.changeBasePrice(data.basePrice());
        return service;
    }

    @Transactional
    public ServiceOffering changeServiceActivation(String code, boolean active) {
        ServiceOffering service = getServiceByCode(code);
        if (active) {
            service.activate();
        } else {
            service.deactivate();
        }
        return service;
    }

    private ServiceCategory requireCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record NewService(
            Long categoryId,
            String code,
            String name,
            String description,
            BigDecimal basePrice,
            int durationMinutes,
            int bufferMinutes,
            ResourceType requiredResourceType) {
    }

    public record ServiceUpdate(
            Long categoryId,
            String name,
            String description,
            BigDecimal basePrice,
            int durationMinutes,
            int bufferMinutes,
            ResourceType requiredResourceType) {
    }
}
