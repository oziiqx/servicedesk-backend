package pl.servicedesk.demo;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.catalog.domain.ServiceCategory;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.catalog.service.CatalogService;
import pl.servicedesk.catalog.service.CatalogService.NewService;
import pl.servicedesk.identity.service.RegistrationService;
import pl.servicedesk.identity.service.RegistrationService.RegisterClientCommand;
import pl.servicedesk.resources.domain.ResourceType;
import pl.servicedesk.resources.service.ResourceService;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.service.EmployeeService;
import pl.servicedesk.staff.service.EmployeeService.OnboardCommand;
import pl.servicedesk.staff.service.EmployeeSkillService;
import pl.servicedesk.staff.service.WorkingHoursService;
import pl.servicedesk.staff.service.WorkingHoursService.DayShift;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
class DemoDataInitializer implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "Sup3rSecret";

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final CatalogService catalogService;
    private final ResourceService resourceService;
    private final EmployeeService employeeService;
    private final EmployeeSkillService employeeSkillService;
    private final WorkingHoursService workingHoursService;
    private final RegistrationService registrationService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (serviceOfferingRepository.count() > 0) {
            log.info("Demo data already present; skipping the demo initializer.");
            return;
        }

        ServiceCategory diagnostics = catalogService.createCategory("Diagnostics", "Inspections and check-ups", 1);
        ServiceCategory repairs = catalogService.createCategory("Repairs", "Repair and replacement work", 2);
        catalogService.createService(new NewService(diagnostics.getId(), "DIAG-STD", "Standard diagnostics",
                "45-minute inspection", new BigDecimal("149.99"), 45, 15, ResourceType.STATION));
        catalogService.createService(new NewService(repairs.getId(), "BRAKE-PADS", "Brake pad replacement",
                null, new BigDecimal("320.00"), 90, 15, ResourceType.BAY));
        catalogService.createService(new NewService(diagnostics.getId(), "CONSULT", "Advisory consultation",
                null, new BigDecimal("80.00"), 30, 0, null));

        resourceService.create("Station 1", ResourceType.STATION);
        resourceService.create("Bay A", ResourceType.BAY);

        Employee senior = employeeService.onboard(new OnboardCommand("olga@servicedesk.local", DEMO_PASSWORD,
                "Olga", "Mistrz", null, "Olga Mistrz", "Senior specialist", LocalDate.of(2022, 3, 1)));
        employeeSkillService.replaceSkills(senior.getId(), Set.of("DIAG-STD", "BRAKE-PADS", "CONSULT"));
        workingHoursService.replaceSchedule(senior.getId(), weekdays(LocalTime.of(8, 0), LocalTime.of(16, 0)));

        Employee junior = employeeService.onboard(new OnboardCommand("piotr@servicedesk.local", DEMO_PASSWORD,
                "Piotr", "Junior", null, "Piotr Junior", "Specialist", LocalDate.of(2024, 9, 1)));
        employeeSkillService.replaceSkills(junior.getId(), Set.of("DIAG-STD", "CONSULT"));
        workingHoursService.replaceSchedule(junior.getId(), weekdays(LocalTime.of(10, 0), LocalTime.of(18, 0)));

        registrationService.register(new RegisterClientCommand("client@servicedesk.local", DEMO_PASSWORD,
                "Kasia", "Klient", null, true));

        log.info("Demo data created: 2 categories, 3 services, 2 resources, 2 employees, 1 client. "
                + "Every demo account uses the password '{}'.", DEMO_PASSWORD);
    }

    private List<DayShift> weekdays(LocalTime from, LocalTime to) {
        return Stream.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY)
                .map(day -> new DayShift(day, from, to))
                .toList();
    }
}
