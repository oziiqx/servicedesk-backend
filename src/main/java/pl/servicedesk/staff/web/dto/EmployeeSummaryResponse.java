package pl.servicedesk.staff.web.dto;

public record EmployeeSummaryResponse(
        Long id,
        String displayName,
        String title) {
}
