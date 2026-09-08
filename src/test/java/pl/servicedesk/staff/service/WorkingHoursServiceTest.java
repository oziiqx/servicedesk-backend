package pl.servicedesk.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeWorkingHours;
import pl.servicedesk.staff.repository.EmployeeWorkingHoursRepository;
import pl.servicedesk.staff.service.WorkingHoursService.DayShift;

@ExtendWith(MockitoExtension.class)
class WorkingHoursServiceTest {

    @Mock
    private EmployeeWorkingHoursRepository workingHoursRepository;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private WorkingHoursService workingHoursService;

    @Test
    void replacesTheWholeWeekInOneOperation() {
        Employee employee = employee();
        given(employeeService.getById(1L)).willReturn(employee);
        given(workingHoursRepository.saveAll(anyList())).willAnswer(call -> call.getArgument(0));

        workingHoursService.replaceSchedule(1L, List.of(
                new DayShift(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                new DayShift(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))));

        InOrder inOrder = inOrder(workingHoursRepository);
        inOrder.verify(workingHoursRepository).deleteByEmployeeId(1L);
        inOrder.verify(workingHoursRepository).flush();

        ArgumentCaptor<List<EmployeeWorkingHours>> captor = ArgumentCaptor.forClass(List.class);
        inOrder.verify(workingHoursRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .hasSize(2)
                .allSatisfy(entry -> assertThat(entry.getEmployee()).isSameAs(employee));
    }

    @Test
    void rejectsTwoShiftsOnTheSameDay() {
        assertThatThrownBy(() -> workingHoursService.replaceSchedule(1L, List.of(
                new DayShift(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                new DayShift(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0)))))
                .isInstanceOf(BusinessRuleViolationException.class);

        verifyNoInteractions(workingHoursRepository, employeeService);
    }

    @Test
    void rejectsShiftThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> workingHoursService.replaceSchedule(1L, List.of(
                new DayShift(DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(9, 0)))))
                .isInstanceOf(BusinessRuleViolationException.class);

        verifyNoInteractions(workingHoursRepository, employeeService);
    }

    private static Employee employee() {
        return new Employee(User.activeUser("e@example.com", "h", "A", "B", null),
                "Dr B", "Dentist", LocalDate.of(2024, 1, 1));
    }
}
