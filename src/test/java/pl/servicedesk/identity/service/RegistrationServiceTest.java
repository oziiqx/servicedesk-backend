package pl.servicedesk.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.clients.repository.ClientRepository;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.RoleRepository;
import pl.servicedesk.identity.repository.UserRepository;
import pl.servicedesk.identity.service.RegistrationService.RegisterClientCommand;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void createsClientAccountWithEncodedPasswordAndClientRole() {
        RegisterClientCommand command = new RegisterClientCommand(
                "  New.User@Example.com ", "Sup3rSecret", " Ada ", " Kowalska ", " 600 700 800 ", true);
        given(userRepository.existsByEmailIgnoreCase("new.user@example.com")).willReturn(false);
        given(roleRepository.findByName(RoleName.CLIENT)).willReturn(Optional.of(new Role(RoleName.CLIENT)));
        given(passwordEncoder.encode("Sup3rSecret")).willReturn("encoded-hash");

        registrationService.register(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("new.user@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(savedUser.getFirstName()).isEqualTo("Ada");
        assertThat(savedUser.getPhone()).isEqualTo("600 700 800");
        assertThat(savedUser.getRoles()).extracting(Role::getName).containsExactly(RoleName.CLIENT);

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());
        assertThat(clientCaptor.getValue().isMarketingConsent()).isTrue();
    }

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() {
        RegisterClientCommand command = new RegisterClientCommand(
                "taken@example.com", "Sup3rSecret", "Ada", "Kowalska", null, false);
        given(userRepository.existsByEmailIgnoreCase("taken@example.com")).willReturn(true);

        assertThatThrownBy(() -> registrationService.register(command))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken@example.com");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(clientRepository, passwordEncoder);
    }
}
