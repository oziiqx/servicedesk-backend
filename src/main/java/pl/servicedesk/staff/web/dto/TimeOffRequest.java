package pl.servicedesk.staff.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record TimeOffRequest(
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Size(max = 255) String reason) {
}
