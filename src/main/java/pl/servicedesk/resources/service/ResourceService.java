package pl.servicedesk.resources.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.resources.domain.ResourceType;
import pl.servicedesk.resources.repository.BookableResourceRepository;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final BookableResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public List<BookableResource> list(boolean includeInactive) {
        return includeInactive
                ? resourceRepository.findAllByOrderByNameAsc()
                : resourceRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public BookableResource getById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", id));
    }

    @Transactional
    public BookableResource create(String name, ResourceType type) {
        String normalized = name.trim();
        rejectDuplicateName(normalized);
        return resourceRepository.save(new BookableResource(normalized, type));
    }

    @Transactional
    public BookableResource update(Long id, String name, ResourceType type) {
        String normalized = name.trim();
        BookableResource resource = getById(id);
        if (!resource.getName().equalsIgnoreCase(normalized) && resourceRepository.existsByNameIgnoreCase(normalized)) {
            throw new DuplicateResourceException("Resource named '%s' already exists".formatted(normalized));
        }
        resource.update(normalized, type);
        return resource;
    }

    @Transactional
    public BookableResource changeActivation(Long id, boolean active) {
        BookableResource resource = getById(id);
        if (active) {
            resource.activate();
        } else {
            resource.deactivate();
        }
        return resource;
    }

    private void rejectDuplicateName(String name) {
        if (resourceRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Resource named '%s' already exists".formatted(name));
        }
    }
}
