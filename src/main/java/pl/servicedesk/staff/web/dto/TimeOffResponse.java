package pl.servicedesk.staff.web.dto;

import java.time.Instant;

public record TimeOffResponse(
        Long id,
        Instant startsAt,
        Instant endsAt,
        String reason) {
}
