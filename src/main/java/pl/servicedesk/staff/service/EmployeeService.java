package pl.servicedesk.staff.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.common.error.DuplicateResourceException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.identity.domain.Role;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.identity.repository.RoleRepository;
import pl.servicedesk.identity.repository.UserRepository;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmploymentStatus;
import pl.servicedesk.staff.repository.EmployeeRepository;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Employee onboard(OnboardCommand command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with email %s already exists".formatted(email));
        }

        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("EMPLOYEE role is not seeded"));

        User user = User.activeUser(
                email,
                passwordEncoder.encode(command.password()),
                command.firstName().trim(),
                command.lastName().trim(),
                command.phone() == null || command.phone().isBlank() ? null : command.phone().trim());
        user.grantRole(employeeRole);
        userRepository.save(user);

        Employee employee = new Employee(user, command.displayName().trim(), trimToNull(command.title()),
                command.hiredOn());
        return employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public List<Employee> list(EmploymentStatus status) {
        return status == null
                ? employeeRepository.findAllByOrderByDisplayNameAsc()
                : employeeRepository.findAllByStatusOrderByDisplayNameAsc(status);
    }

    @Transactional(readOnly = true)
    public Employee getById(Long id) {
        return employeeRepository.findWithUserById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
    }

    @Transactional(readOnly = true)
    public Employee requireByUserId(Long userId) {
        return employeeRepository.findWithUserByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("The current account is not an employee"));
    }

    @Transactional
    public Employee updateProfile(Long id, String displayName, String title) {
        Employee employee = getById(id);
        employee.updateProfile(displayName.trim(), trimToNull(title));
        return employee;
    }

    @Transactional
    public Employee changeStatus(Long id, EmploymentStatus status) {
        Employee employee = getById(id);
        employee.changeStatus(status);
        return employee;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record OnboardCommand(
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            String displayName,
            String title,
            LocalDate hiredOn) {
    }
}
