package pl.servicedesk.scheduling.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.scheduling.service.AvailabilityService;
import pl.servicedesk.scheduling.web.dto.AvailableSlotResponse;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
@Tag(name = "Availability")
class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final AppointmentMapper appointmentMapper;

    @GetMapping
    List<AvailableSlotResponse> openSlots(@RequestParam Long employeeId,
                                          @RequestParam String serviceCode,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentMapper.toSlotResponses(
                availabilityService.findOpenSlots(employeeId, serviceCode, date));
    }
}
