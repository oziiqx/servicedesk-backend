package pl.servicedesk.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.identity.domain.RefreshToken;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.RefreshTokenRepository;
import pl.servicedesk.identity.security.AuthenticatedUser;
import pl.servicedesk.identity.security.InvalidRefreshTokenException;
import pl.servicedesk.identity.security.JwtProperties;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IssuedToken issueFor(User user, String userAgent) {
        String rawValue = generateRawValue();
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(user, hash(rawValue), userAgent, issuedAt, expiresAt));
        return new IssuedToken(rawValue, expiresAt);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotationResult rotate(String presentedValue, String userAgent) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(presentedValue))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is not recognized"));

        Instant now = clock.instant();
        if (stored.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllActiveForUser(stored.getUser(), now);
            throw new InvalidRefreshTokenException("Refresh token has already been used");
        }
        if (!stored.isActiveAt(now)) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        stored.revokeAt(now);
        User owner = stored.getUser();
        return new RotationResult(AuthenticatedUser.forLogin(owner), issueFor(owner, userAgent));
    }

    @Transactional
    public void revoke(String presentedValue) {
        refreshTokenRepository.findByTokenHash(hash(presentedValue))
                .ifPresent(token -> token.revokeAt(clock.instant()));
    }

    private String generateRawValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public record RotationResult(AuthenticatedUser principal, IssuedToken newToken) {
    }
}
