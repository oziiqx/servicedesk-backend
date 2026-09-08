package pl.servicedesk.identity.bootstrap;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.RoleRepository;
import pl.servicedesk.identity.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRoles_Name(RoleName.ADMIN)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is not seeded"));

        User admin = User.activeUser(
                properties.adminEmail().trim().toLowerCase(Locale.ROOT),
                passwordEncoder.encode(properties.adminPassword()),
                properties.adminFirstName().trim(),
                properties.adminLastName().trim(),
                null);
        admin.grantRole(adminRole);
        userRepository.save(admin);

        log.warn("Created the bootstrap ADMIN account '{}'. Change its password before exposing the API.",
                admin.getEmail());
        if (properties.usesDefaultPassword()) {
            log.warn("The bootstrap ADMIN is using the built-in default password; "
                    + "set servicedesk.bootstrap.admin-password (or SERVICEDESK_ADMIN_PASSWORD).");
        }
    }
}
