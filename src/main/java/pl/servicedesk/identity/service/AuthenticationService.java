package pl.servicedesk.identity.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.UserRepository;
import pl.servicedesk.identity.security.AuthenticatedUser;
import pl.servicedesk.identity.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Transactional
    public AuthTokens login(String email, String rawPassword, String userAgent) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword));

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Account is no longer available"));

        return issueTokens(user, userAgent);
    }

    public AuthTokens refresh(String refreshToken, String userAgent) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(refreshToken, userAgent);
        JwtService.AccessToken accessToken = jwtService.issueAccessToken(rotation.principal());
        return new AuthTokens(
                accessToken.value(), accessToken.expiresAt(),
                rotation.newToken().value(), rotation.newToken().expiresAt());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthTokens issueTokens(User user, String userAgent) {
        JwtService.AccessToken accessToken = jwtService.issueAccessToken(AuthenticatedUser.forLogin(user));
        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issueFor(user, userAgent);
        return new AuthTokens(
                accessToken.value(), accessToken.expiresAt(),
                refreshToken.value(), refreshToken.expiresAt());
    }

    public record AuthTokens(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt) {
    }
}
