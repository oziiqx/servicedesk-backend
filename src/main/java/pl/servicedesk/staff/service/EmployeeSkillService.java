package pl.servicedesk.staff.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.domain.EmployeeSkill;
import pl.servicedesk.staff.repository.EmployeeSkillRepository;

@Service
@RequiredArgsConstructor
public class EmployeeSkillService {

    private final EmployeeSkillRepository skillRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final EmployeeService employeeService;

    @Transactional(readOnly = true)
    public List<EmployeeSkill> listSkills(Long employeeId) {
        return skillRepository.findByEmployeeIdOrderByServiceOfferingNameAsc(employeeId);
    }

    @Transactional
    public List<EmployeeSkill> replaceSkills(Long employeeId, Set<String> serviceCodes) {
        Employee employee = employeeService.getById(employeeId);
        Set<String> requested = new LinkedHashSet<>(serviceCodes);

        List<ServiceOffering> services = serviceRepository.findByCodeIn(requested);
        if (services.size() != requested.size()) {
            Set<String> known = services.stream().map(ServiceOffering::getCode).collect(Collectors.toSet());
            requested.removeAll(known);
            throw new ResourceNotFoundException("Unknown service codes: " + String.join(", ", requested));
        }

        skillRepository.deleteByEmployeeId(employeeId);
        skillRepository.flush();

        return skillRepository.saveAll(services.stream()
                .map(service -> new EmployeeSkill(employee, service))
                .toList());
    }
}
