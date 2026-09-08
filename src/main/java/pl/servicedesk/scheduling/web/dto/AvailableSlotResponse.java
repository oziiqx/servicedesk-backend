package pl.servicedesk.scheduling.web.dto;

import java.time.Instant;

public record AvailableSlotResponse(
        Instant start,
        Instant end) {
}
