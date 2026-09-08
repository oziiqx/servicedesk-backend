package pl.servicedesk.clients.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.clients.service.ClientProfileService;
import pl.servicedesk.clients.service.ClientProfileService.ClientProfile;
import pl.servicedesk.clients.web.dto.ClientProfileResponse;
import pl.servicedesk.identity.security.AuthenticatedUser;
import pl.servicedesk.identity.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/clients/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client profile")
class ClientProfileController {

    private final ClientProfileService clientProfileService;

    @GetMapping
    ClientProfileResponse me(@CurrentUser AuthenticatedUser user) {
        ClientProfile profile = clientProfileService.forUser(user.id());
        return new ClientProfileResponse(
                profile.email(),
                profile.fullName(),
                profile.marketingConsent(),
                profile.loyaltyTier(),
                profile.completedAppointments(),
                profile.lifetimeSpend());
    }
}
