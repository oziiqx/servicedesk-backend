package pl.servicedesk.identity.web.dto;

import java.util.List;

public record CurrentUserResponse(
        Long userId,
        String email,
        List<String> roles) {
}
