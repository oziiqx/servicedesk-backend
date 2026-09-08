package pl.servicedesk.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.catalog.domain.ServiceCategory;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeSkill;
import pl.servicedesk.staff.repository.EmployeeSkillRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeSkillServiceTest {

    @Mock
    private EmployeeSkillRepository skillRepository;

    @Mock
    private ServiceOfferingRepository serviceRepository;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeSkillService skillService;

    @Test
    void replacesTheSkillSetWithTheRequestedServices() {
        Employee employee = employee();
        given(employeeService.getById(1L)).willReturn(employee);
        given(serviceRepository.findByCodeIn(any())).willReturn(List.of(service("DIAG"), service("REPAIR")));
        given(skillRepository.saveAll(anyList())).willAnswer(call -> call.getArgument(0));

        skillService.replaceSkills(1L, new LinkedHashSet<>(List.of("DIAG", "REPAIR")));

        verify(skillRepository).deleteByEmployeeId(1L);
        verify(skillRepository).flush();

        ArgumentCaptor<List<EmployeeSkill>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .hasSize(2)
                .allSatisfy(skill -> assertThat(skill.getEmployee()).isSameAs(employee));
    }

    @Test
    void rejectsUnknownServiceCodes() {
        given(employeeService.getById(1L)).willReturn(employee());
        given(serviceRepository.findByCodeIn(any())).willReturn(List.of(service("DIAG")));

        assertThatThrownBy(() -> skillService.replaceSkills(1L, new LinkedHashSet<>(List.of("DIAG", "MISSING"))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MISSING");

        verify(skillRepository, never()).saveAll(any());
    }

    private static Employee employee() {
        return new Employee(User.activeUser("e@example.com", "h", "A", "B", null),
                "Dr B", "Dentist", LocalDate.of(2024, 1, 1));
    }

    private static ServiceOffering service(String code) {
        return new ServiceOffering(new ServiceCategory("Cat", null, 0), code, "Service " + code, null,
                BigDecimal.TEN, 30, 0, null);
    }
}
