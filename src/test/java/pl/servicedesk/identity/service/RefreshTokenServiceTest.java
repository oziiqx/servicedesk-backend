package pl.servicedesk.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.identity.domain.RefreshToken;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.RefreshTokenRepository;
import pl.servicedesk.identity.security.InvalidRefreshTokenException;
import pl.servicedesk.identity.security.JwtProperties;
import pl.servicedesk.identity.service.RefreshTokenService.RotationResult;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final JwtProperties jwtProperties = new JwtProperties(
            "integration-secret-value-long-enough-for-hs256", "servicedesk",
            Duration.ofMinutes(15), Duration.ofDays(7));

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties, clock);
    }

    @Test
    void issuesOpaqueTokenAndPersistsOnlyItsHash() {
        User user = client();
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(call -> call.getArgument(0));

        RefreshTokenService.IssuedToken issued = refreshTokenService.issueFor(user, "JUnit");

        assertThat(issued.value()).isNotBlank();
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));

        RefreshToken persisted = captureSaved();
        assertThat(persisted.getTokenHash()).hasSize(64).isNotEqualTo(issued.value());
        assertThat(persisted.getUserAgent()).isEqualTo("JUnit");
    }

    @Test
    void rejectsUnknownRefreshToken() {
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown", "JUnit"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokesWholeTokenFamilyWhenAlreadyUsedTokenIsPresentedAgain() {
        User user = client();
        RefreshToken reused = new RefreshToken(user, "stored-hash", "JUnit",
                NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(6)));
        reused.revokeAt(NOW.minusSeconds(30));
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(reused));

        assertThatThrownBy(() -> refreshTokenService.rotate("reused", "JUnit"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).findByTokenHash(anyString());
        verify(refreshTokenRepository).revokeAllActiveForUser(user, NOW);
        verifyNoMoreInteractions(refreshTokenRepository);
    }

    @Test
    void rotatesActiveTokenByRevokingItAndIssuingAReplacement() {
        User user = client();
        RefreshToken active = new RefreshToken(user, "stored-hash", "JUnit",
                NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofDays(6)));
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(active));
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(call -> call.getArgument(0));

        RotationResult result = refreshTokenService.rotate("presented", "JUnit");

        assertThat(active.getRevokedAt()).isEqualTo(NOW);
        assertThat(result.principal().getUsername()).isEqualTo("client@example.com");
        assertThat(result.newToken().value()).isNotBlank();
        assertThat(captureSaved().getTokenHash()).isNotEqualTo("stored-hash");
    }

    private RefreshToken captureSaved() {
        var captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        return captor.getValue();
    }

    private User client() {
        User user = User.activeUser("client@example.com", "hash", "Ola", "Nowak", null);
        user.grantRole(new Role(RoleName.CLIENT));
        return user;
    }
}
