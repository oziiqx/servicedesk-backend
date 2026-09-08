package pl.servicedesk.resources.web;

import java.util.List;
import org.mapstruct.Mapper;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.resources.web.dto.ResourceResponse;

@Mapper
interface ResourceMapper {

    ResourceResponse toResponse(BookableResource resource);

    List<ResourceResponse> toResponses(List<BookableResource> resources);
}
