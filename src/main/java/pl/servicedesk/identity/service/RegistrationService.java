package pl.servicedesk.identity.service;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.clients.repository.ClientRepository;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.RoleRepository;
import pl.servicedesk.identity.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisteredAccount register(RegisterClientCommand command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with email %s already exists".formatted(email));
        }

        Role clientRole = roleRepository.findByName(RoleName.CLIENT)
                .orElseThrow(() -> new IllegalStateException("CLIENT role is not seeded"));

        User user = User.activeUser(
                email,
                passwordEncoder.encode(command.rawPassword()),
                command.firstName().trim(),
                command.lastName().trim(),
                normalizePhone(command.phone()));
        user.grantRole(clientRole);
        userRepository.save(user);

        clientRepository.save(new Client(user, command.marketingConsent()));
        return new RegisteredAccount(user.getId(), user.getEmail(), user.fullName());
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }

    public record RegisterClientCommand(
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            String phone,
            boolean marketingConsent) {
    }

    public record RegisteredAccount(Long userId, String email, String fullName) {
    }
}
