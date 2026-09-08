package pl.servicedesk.identity.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.servicedesk.identity.security.AuthenticatedUser;
import pl.servicedesk.identity.security.CurrentUser;
import pl.servicedesk.identity.web.dto.CurrentUserResponse;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Authentication")
class CurrentUserController {

    @GetMapping
    CurrentUserResponse currentUser(@CurrentUser AuthenticatedUser user) {
        return new CurrentUserResponse(
                user.id(),
                user.getUsername(),
                user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}
