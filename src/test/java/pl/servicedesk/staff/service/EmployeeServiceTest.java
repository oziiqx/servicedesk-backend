package pl.servicedesk.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.RoleRepository;
import pl.servicedesk.identity.repository.UserRepository;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmploymentStatus;
import pl.servicedesk.staff.repository.EmployeeRepository;
import pl.servicedesk.staff.service.EmployeeService.OnboardCommand;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void onboardsEmployeeAsActiveAccountWithEmployeeRole() {
        OnboardCommand command = new OnboardCommand(
                "  New.Emp@Example.com ", "Sup3rSecret", " Anna ", " Lis ", " 111 222 333 ",
                " Dr Anna Lis ", "  ", LocalDate.of(2024, 1, 15));
        given(userRepository.existsByEmailIgnoreCase("new.emp@example.com")).willReturn(false);
        given(roleRepository.findByName(RoleName.EMPLOYEE)).willReturn(Optional.of(new Role(RoleName.EMPLOYEE)));
        given(passwordEncoder.encode("Sup3rSecret")).willReturn("encoded-hash");
        given(employeeRepository.save(any(Employee.class))).willAnswer(call -> call.getArgument(0));

        Employee employee = employeeService.onboard(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("new.emp@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(userCaptor.getValue().getPhone()).isEqualTo("111 222 333");
        assertThat(userCaptor.getValue().getRoles()).extracting(Role::getName).containsExactly(RoleName.EMPLOYEE);

        assertThat(employee.getDisplayName()).isEqualTo("Dr Anna Lis");
        assertThat(employee.getTitle()).isNull();
        assertThat(employee.getStatus()).isEqualTo(EmploymentStatus.ACTIVE);
    }

    @Test
    void rejectsOnboardingWhenEmailIsAlreadyRegistered() {
        OnboardCommand command = new OnboardCommand(
                "taken@example.com", "Sup3rSecret", "Anna", "Lis", null, "Dr Lis", null, LocalDate.now());
        given(userRepository.existsByEmailIgnoreCase("taken@example.com")).willReturn(true);

        assertThatThrownBy(() -> employeeService.onboard(command))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void changesEmploymentStatus() {
        Employee employee = new Employee(
                User.activeUser("e@example.com", "h", "A", "B", null), "Dr B", "Dentist", LocalDate.now());
        given(employeeRepository.findWithUserById(3L)).willReturn(Optional.of(employee));

        employeeService.changeStatus(3L, EmploymentStatus.SUSPENDED);

        assertThat(employee.getStatus()).isEqualTo(EmploymentStatus.SUSPENDED);
    }
}
