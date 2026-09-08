package pl.servicedesk.staff.web.dto;

import java.time.LocalTime;

public record WorkingHoursResponse(
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {
}
