package pl.servicedesk.scheduling.web;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentItem;
import pl.servicedesk.scheduling.service.AvailabilityService.AvailableSlot;
import pl.servicedesk.scheduling.web.dto.AppointmentItemResponse;
import pl.servicedesk.scheduling.web.dto.AppointmentResponse;
import pl.servicedesk.scheduling.web.dto.AvailableSlotResponse;

@Mapper
interface AppointmentMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client")
    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.displayName")
    @Mapping(target = "resourceName", source = "resource.name")
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponses(List<Appointment> appointments);

    @Mapping(target = "serviceCode", source = "service.code")
    @Mapping(target = "serviceName", source = "service.name")
    AppointmentItemResponse toItemResponse(AppointmentItem item);

    AvailableSlotResponse toResponse(AvailableSlot slot);

    List<AvailableSlotResponse> toSlotResponses(List<AvailableSlot> slots);

    default String clientDisplayName(Client client) {
        return client.getUser().fullName();
    }
}
