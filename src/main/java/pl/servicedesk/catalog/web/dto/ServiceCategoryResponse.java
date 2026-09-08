package pl.servicedesk.catalog.web.dto;

public record ServiceCategoryResponse(
        Long id,
        String name,
        String description,
        int displayOrder) {
}
