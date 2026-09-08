package pl.servicedesk.resources.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.resources.service.ResourceService;
import pl.servicedesk.resources.web.dto.CreateResourceRequest;
import pl.servicedesk.resources.web.dto.ResourceResponse;
import pl.servicedesk.resources.web.dto.UpdateResourceRequest;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Resources")
class ResourceController {

    private final ResourceService resourceService;
    private final ResourceMapper resourceMapper;

    @GetMapping("/resources")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    List<ResourceResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return resourceMapper.toResponses(resourceService.list(includeInactive));
    }

    @GetMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    ResourceResponse getById(@PathVariable Long id) {
        return resourceMapper.toResponse(resourceService.getById(id));
    }

    @PostMapping("/admin/resources")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResourceResponse> create(@Valid @RequestBody CreateResourceRequest request) {
        ResourceResponse created = resourceMapper.toResponse(
                resourceService.create(request.name(), request.type()));
        return ResponseEntity.created(URI.create("/api/v1/resources/" + created.id())).body(created);
    }

    @PutMapping("/admin/resources/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResourceResponse update(@PathVariable Long id, @Valid @RequestBody UpdateResourceRequest request) {
        return resourceMapper.toResponse(resourceService.update(id, request.name(), request.type()));
    }

    @PostMapping("/admin/resources/{id}/activation")
    @PreAuthorize("hasRole('ADMIN')")
    ResourceResponse changeActivation(@PathVariable Long id, @RequestParam boolean active) {
        return resourceMapper.toResponse(resourceService.changeActivation(id, active));
    }
}
