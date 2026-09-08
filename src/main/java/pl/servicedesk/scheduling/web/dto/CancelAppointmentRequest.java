package pl.servicedesk.scheduling.web.dto;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @Size(max = 255) String reason) {
}
