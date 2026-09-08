package pl.servicedesk.scheduling.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record BookAppointmentRequest(
        @NotNull Long employeeId,
        @NotEmpty @Valid List<ServiceLine> services,
        @NotNull @Future Instant scheduledStart,
        Long resourceId) {

    public record ServiceLine(
            @NotBlank String serviceCode,
            @Min(1) int quantity) {
    }
}
