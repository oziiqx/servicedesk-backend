package pl.servicedesk.identity.web.dto;

import java.time.Instant;

public record AuthTokensResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String tokenType) {
}
