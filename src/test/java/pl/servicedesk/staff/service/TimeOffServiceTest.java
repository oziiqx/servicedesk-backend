package pl.servicedesk.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.identity.domain.User;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeTimeOff;
import pl.servicedesk.staff.repository.EmployeeTimeOffRepository;

@ExtendWith(MockitoExtension.class)
class TimeOffServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-01T00:00:00Z");

    @Mock
    private EmployeeTimeOffRepository timeOffRepository;

    @Mock
    private EmployeeService employeeService;

    private TimeOffService timeOffService;

    @BeforeEach
    void setUp() {
        timeOffService = new TimeOffService(timeOffRepository, employeeService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsFutureTimeOffAndTrimsReason() {
        Instant start = Instant.parse("2026-03-01T08:00:00Z");
        Instant end = Instant.parse("2026-03-05T16:00:00Z");
        given(timeOffRepository.existsByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(1L, end, start))
                .willReturn(false);
        given(employeeService.getById(1L)).willReturn(employee());
        given(timeOffRepository.save(any(EmployeeTimeOff.class))).willAnswer(call -> call.getArgument(0));

        EmployeeTimeOff result = timeOffService.request(1L, start, end, "  Vacation  ");

        assertThat(result.getReason()).isEqualTo("Vacation");
        assertThat(result.getStartsAt()).isEqualTo(start);
        assertThat(result.getEndsAt()).isEqualTo(end);
    }

    @Test
    void rejectsTimeOffStartingInThePast() {
        assertThatThrownBy(() -> timeOffService.request(1L,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"), null))
                .isInstanceOf(BusinessRuleViolationException.class);

        verifyNoInteractions(employeeService, timeOffRepository);
    }

    @Test
    void rejectsTimeOffThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> timeOffService.request(1L,
                Instant.parse("2026-03-05T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z"), null))
                .isInstanceOf(BusinessRuleViolationException.class);

        verifyNoInteractions(employeeService, timeOffRepository);
    }

    @Test
    void rejectsTimeOffOverlappingAnExistingEntry() {
        Instant start = Instant.parse("2026-03-01T08:00:00Z");
        Instant end = Instant.parse("2026-03-05T16:00:00Z");
        given(timeOffRepository.existsByEmployeeIdAndStartsAtLessThanAndEndsAtGreaterThan(1L, end, start))
                .willReturn(true);

        assertThatThrownBy(() -> timeOffService.request(1L, start, end, null))
                .isInstanceOf(BusinessRuleViolationException.class);

        verifyNoInteractions(employeeService);
    }

    @Test
    void cancellingUnknownEntryThrows() {
        given(timeOffRepository.findByIdAndEmployeeId(9L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> timeOffService.cancel(1L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Employee employee() {
        return new Employee(User.activeUser("e@example.com", "h", "A", "B", null),
                "Dr B", "Dentist", LocalDate.of(2024, 1, 1));
    }
}
