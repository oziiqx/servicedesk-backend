package pl.servicedesk.catalog.web;

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
import pl.servicedesk.catalog.service.CatalogService;
import pl.servicedesk.catalog.service.CatalogService.NewService;
import pl.servicedesk.catalog.service.CatalogService.ServiceUpdate;
import pl.servicedesk.catalog.web.dto.CreateServiceRequest;
import pl.servicedesk.catalog.web.dto.ServiceCategoryResponse;
import pl.servicedesk.catalog.web.dto.ServiceResponse;
import pl.servicedesk.catalog.web.dto.UpdateServiceRequest;
import pl.servicedesk.catalog.web.dto.UpsertCategoryRequest;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Service catalog")
class ServiceCatalogController {

    private final CatalogService catalogService;
    private final CatalogMapper catalogMapper;

    @GetMapping("/services")
    List<ServiceResponse> listServices(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return catalogMapper.toServiceResponses(catalogService.listServices(includeInactive));
    }

    @GetMapping("/services/{code}")
    ServiceResponse getService(@PathVariable String code) {
        return catalogMapper.toResponse(catalogService.getServiceByCode(code));
    }

    @GetMapping("/service-categories")
    List<ServiceCategoryResponse> listCategories() {
        return catalogMapper.toCategoryResponses(catalogService.listCategories());
    }

    @PostMapping("/admin/service-categories")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ServiceCategoryResponse> createCategory(@Valid @RequestBody UpsertCategoryRequest request) {
        ServiceCategoryResponse created = catalogMapper.toResponse(catalogService.createCategory(
                request.name(), request.description(), request.displayOrder()));
        return ResponseEntity.created(URI.create("/api/v1/service-categories/" + created.id())).body(created);
    }

    @PutMapping("/admin/service-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ServiceCategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody UpsertCategoryRequest request) {
        return catalogMapper.toResponse(catalogService.updateCategory(
                id, request.name(), request.description(), request.displayOrder()));
    }

    @PostMapping("/admin/services")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ServiceResponse> createService(@Valid @RequestBody CreateServiceRequest request) {
        ServiceResponse created = catalogMapper.toResponse(catalogService.createService(new NewService(
                request.categoryId(),
                request.code(),
                request.name(),
                request.description(),
                request.basePrice(),
                request.durationMinutes(),
                request.bufferMinutes(),
                request.requiredResourceType())));
        return ResponseEntity.created(URI.create("/api/v1/services/" + created.code())).body(created);
    }

    @PutMapping("/admin/services/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    ServiceResponse updateService(@PathVariable String code, @Valid @RequestBody UpdateServiceRequest request) {
        return catalogMapper.toResponse(catalogService.updateService(code, new ServiceUpdate(
                request.categoryId(),
                request.name(),
                request.description(),
                request.basePrice(),
                request.durationMinutes(),
                request.bufferMinutes(),
                request.requiredResourceType())));
    }

    @PostMapping("/admin/services/{code}/activation")
    @PreAuthorize("hasRole('ADMIN')")
    ServiceResponse changeActivation(@PathVariable String code, @RequestParam boolean active) {
        return catalogMapper.toResponse(catalogService.changeServiceActivation(code, active));
    }
}
