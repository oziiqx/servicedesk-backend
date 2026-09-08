package pl.servicedesk.catalog.web;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.servicedesk.catalog.domain.ServiceCategory;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.web.dto.ServiceCategoryResponse;
import pl.servicedesk.catalog.web.dto.ServiceResponse;

@Mapper
interface CatalogMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    ServiceResponse toResponse(ServiceOffering service);

    List<ServiceResponse> toServiceResponses(List<ServiceOffering> services);

    ServiceCategoryResponse toResponse(ServiceCategory category);

    List<ServiceCategoryResponse> toCategoryResponses(List<ServiceCategory> categories);
}
