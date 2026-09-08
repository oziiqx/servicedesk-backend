package pl.servicedesk.staff.web.dto;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        Long userId,
        String email,
        String fullName,
        String displayName,
        String title,
        String status,
        LocalDate hiredOn) {
}
