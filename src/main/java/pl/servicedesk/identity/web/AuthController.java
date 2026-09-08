package pl.servicedesk.identity.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.identity.service.AuthenticationService;
import pl.servicedesk.identity.service.AuthenticationService.AuthTokens;
import pl.servicedesk.identity.service.RegistrationService;
import pl.servicedesk.identity.service.RegistrationService.RegisterClientCommand;
import pl.servicedesk.identity.service.RegistrationService.RegisteredAccount;
import pl.servicedesk.identity.web.dto.AuthTokensResponse;
import pl.servicedesk.identity.web.dto.LoginRequest;
import pl.servicedesk.identity.web.dto.LogoutRequest;
import pl.servicedesk.identity.web.dto.RefreshRequest;
import pl.servicedesk.identity.web.dto.RegisterRequest;
import pl.servicedesk.identity.web.dto.RegisteredAccountResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegisteredAccountResponse register(@Valid @RequestBody RegisterRequest request) {
        RegisteredAccount account = registrationService.register(new RegisterClientCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.marketingConsent()));
        return new RegisteredAccountResponse(account.userId(), account.email(), account.fullName());
    }

    @PostMapping("/login")
    AuthTokensResponse login(@Valid @RequestBody LoginRequest request,
                             @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return toResponse(authenticationService.login(request.email(), request.password(), userAgent));
    }

    @PostMapping("/refresh")
    AuthTokensResponse refresh(@Valid @RequestBody RefreshRequest request,
                               @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return toResponse(authenticationService.refresh(request.refreshToken(), userAgent));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request.refreshToken());
    }

    private AuthTokensResponse toResponse(AuthTokens tokens) {
        return new AuthTokensResponse(
                tokens.accessToken(),
                tokens.accessTokenExpiresAt(),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresAt(),
                "Bearer");
    }
}
