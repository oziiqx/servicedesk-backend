package pl.servicedesk.scheduling.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.servicedesk.catalog.domain.ServiceOffering;
import pl.servicedesk.catalog.repository.ServiceOfferingRepository;
import pl.servicedesk.clients.domain.Client;
import pl.servicedesk.clients.repository.ClientRepository;
import pl.servicedesk.common.error.BusinessRuleViolationException;
import pl.servicedesk.common.error.ResourceNotFoundException;
import pl.servicedesk.common.error.SlotUnavailableException;
import pl.servicedesk.pricing.AppointmentPricer;
import pl.servicedesk.pricing.AppointmentPricer.AppointmentPrice;
import pl.servicedesk.pricing.AppointmentPricer.LineItem;
import pl.servicedesk.resources.domain.BookableResource;
import pl.servicedesk.resources.domain.ResourceType;
import pl.servicedesk.resources.repository.BookableResourceRepository;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;
import pl.servicedesk.scheduling.repository.AppointmentRepository;
import pl.servicedesk.staff.domain.Employee;
import pl.servicedesk.staff.repository.EmployeeRepository;

@Service
@RequiredArgsConstructor
public class AppointmentBookingService {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final BookableResourceRepository resourceRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentPersister appointmentPersister;
    private final BookingPolicy bookingPolicy;
    private final AppointmentPricer appointmentPricer;

    @Transactional
    public Appointment book(BookCommand command) {
        Client client = clientRepository.findByUserId(command.clientUserId())
                .orElseThrow(() -> new ResourceNotFoundException("The current account is not a client"));

        Employee employee = employeeRepository.findByIdForUpdate(command.employeeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", command.employeeId()));
        if (!employee.isBookable()) {
            throw new BusinessRuleViolationException("The selected employee is not accepting appointments");
        }

        List<RequestedService> requested = resolveServices(command.services());
        Instant start = command.scheduledStart();
        Instant end = start.plus(totalDuration(requested));

        List<String> codes = requested.stream().map(line -> line.service().getCode()).toList();
        bookingPolicy.ensureBookableLeadTime(start);
        bookingPolicy.ensureEmployeeCanPerform(employee.getId(), codes);
        bookingPolicy.ensureWithinWorkingHours(employee.getId(), start, end);
        bookingPolicy.ensureNotOnTimeOff(employee.getId(), start, end);

        BookableResource resource = resolveResource(command.resourceId(), requiredResourceType(requested), start, end);

        bookingPolicy.ensureEmployeeSlotIsFree(employee.getId(), start, end, null);

        AppointmentPrice price = appointmentPricer.price(client, toLineItems(requested), start);

        Appointment appointment = Appointment.schedule(client, employee, resource, start, end);
        requested.forEach(line -> appointment.addItem(line.service(), line.quantity()));
        appointment.applyPricing(price.netAmount(), price.surchargeAmount(), price.discountPercentage(),
                price.discountAmount(), price.totalAmount(), price.loyaltyTier());

        appointmentPersister.persist(appointment);
        return appointmentRepository.findWithDetailsById(appointment.getId()).orElseThrow();
    }

    private List<RequestedService> resolveServices(List<ServiceLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessRuleViolationException("An appointment must include at least one service");
        }
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (ServiceLine line : lines) {
            quantities.merge(line.serviceCode(), line.quantity(), Integer::sum);
        }

        Map<String, ServiceOffering> found = serviceRepository.findByCodeIn(quantities.keySet()).stream()
                .collect(Collectors.toMap(ServiceOffering::getCode, service -> service));

        List<String> unknown = quantities.keySet().stream().filter(code -> !found.containsKey(code)).toList();
        if (!unknown.isEmpty()) {
            throw new ResourceNotFoundException("Unknown service codes: " + String.join(", ", unknown));
        }

        List<RequestedService> resolved = new ArrayList<>();
        quantities.forEach((code, quantity) -> {
            ServiceOffering service = found.get(code);
            if (!service.isActive()) {
                throw new BusinessRuleViolationException("Service '%s' is not available for booking".formatted(code));
            }
            resolved.add(new RequestedService(service, quantity));
        });
        return resolved;
    }

    private Duration totalDuration(List<RequestedService> services) {
        return services.stream()
                .map(line -> line.service().slotLength().multipliedBy(line.quantity()))
                .reduce(Duration.ZERO, Duration::plus);
    }

    private ResourceType requiredResourceType(List<RequestedService> services) {
        Set<ResourceType> types = services.stream()
                .map(line -> line.service().getRequiredResourceType())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (types.size() > 1) {
            throw new BusinessRuleViolationException("The selected services require different resource types");
        }
        return types.stream().findFirst().orElse(null);
    }

    private BookableResource resolveResource(Long requestedResourceId, ResourceType requiredType,
                                             Instant start, Instant end) {
        if (requiredType == null) {
            return null;
        }
        if (requestedResourceId != null) {
            BookableResource resource = resourceRepository.findByIdForUpdate(requestedResourceId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Resource", requestedResourceId));
            if (!resource.isActive() || resource.getType() != requiredType) {
                throw new BusinessRuleViolationException(
                        "Resource '%s' cannot host this appointment".formatted(resource.getName()));
            }
            bookingPolicy.ensureResourceSlotIsFree(resource.getId(), start, end, null);
            return resource;
        }
        return autoAssignResource(requiredType, start, end);
    }

    private BookableResource autoAssignResource(ResourceType requiredType, Instant start, Instant end) {
        for (BookableResource candidate : resourceRepository.findAllByTypeAndActiveTrue(requiredType)) {
            Optional<BookableResource> locked = resourceRepository.findByIdForUpdate(candidate.getId());
            if (locked.isPresent() && !appointmentRepository.existsResourceOverlap(
                    candidate.getId(), start, end, AppointmentStatus.slotBlockingStatuses(), null)) {
                return locked.get();
            }
        }
        throw new SlotUnavailableException(
                "No %s is available for the requested time".formatted(requiredType));
    }

    private List<LineItem> toLineItems(List<RequestedService> services) {
        return services.stream().map(line -> new LineItem(line.service(), line.quantity())).toList();
    }

    private record RequestedService(ServiceOffering service, int quantity) {
    }

    public record BookCommand(
            Long clientUserId,
            Long employeeId,
            List<ServiceLine> services,
            Instant scheduledStart,
            Long resourceId) {
    }

    public record ServiceLine(String serviceCode, int quantity) {
    }
}
