package pl.servicedesk.catalog.web.dto;

import java.math.BigDecimal;

public record ServiceResponse(
        Long id,
        String code,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal basePrice,
        int durationMinutes,
        int bufferMinutes,
        String requiredResourceType,
        boolean active) {
}
