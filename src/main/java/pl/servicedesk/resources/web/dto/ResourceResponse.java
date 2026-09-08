package pl.servicedesk.resources.web.dto;

public record ResourceResponse(
        Long id,
        String name,
        String type,
        boolean active) {
}
