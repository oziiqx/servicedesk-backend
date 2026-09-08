package pl.servicedesk.identity.web.dto;

public record RegisteredAccountResponse(
        Long userId,
        String email,
        String fullName) {
}
