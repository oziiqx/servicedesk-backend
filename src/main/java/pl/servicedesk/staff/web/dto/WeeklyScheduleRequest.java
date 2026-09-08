package pl.servicedesk.staff.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record WeeklyScheduleRequest(
        @NotNull @Valid List<Shift> shifts) {

    public record Shift(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime) {
    }
}
