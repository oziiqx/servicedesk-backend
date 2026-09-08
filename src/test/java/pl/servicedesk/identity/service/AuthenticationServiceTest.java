package pl.servicedesk.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.UserRepository;
import pl.servicedesk.identity.security.JwtService;
import pl.servicedesk.identity.service.AuthenticationService.AuthTokens;
import pl.servicedesk.identity.service.RefreshTokenService.IssuedToken;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void loginIssuesAccessAndRefreshTokensForAuthenticatedAccount() {
        User user = User.activeUser("user@example.com", "hash", "Ola", "Nowak", null);
        user.grantRole(new Role(RoleName.CLIENT));
        given(authenticationManager.authenticate(any())).willReturn(
                new UsernamePasswordAuthenticationToken("user@example.com", "raw-password", List.of()));
        given(userRepository.findByEmailIgnoreCase("user@example.com")).willReturn(Optional.of(user));
        given(jwtService.issueAccessToken(any())).willReturn(
                new JwtService.AccessToken("access-jwt", Instant.parse("2026-01-15T10:15:00Z")));
        given(refreshTokenService.issueFor(eq(user), eq("JUnit"))).willReturn(
                new IssuedToken("refresh-value", Instant.parse("2026-01-22T10:00:00Z")));

        AuthTokens tokens = authenticationService.login("user@example.com", "raw-password", "JUnit");

        assertThat(tokens.accessToken()).isEqualTo("access-jwt");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-value");
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(Instant.parse("2026-01-22T10:00:00Z"));
    }

    @Test
    void loginPropagatesAuthenticationFailureWithoutIssuingTokens() {
        given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authenticationService.login("user@example.com", "wrong", "JUnit"))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService, userRepository);
    }
}
