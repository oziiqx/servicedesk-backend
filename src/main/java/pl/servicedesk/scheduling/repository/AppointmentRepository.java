package pl.servicedesk.scheduling.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.servicedesk.scheduling.domain.Appointment;
import pl.servicedesk.scheduling.domain.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {
            "client", "client.user", "employee", "employee.user", "resource", "items", "items.service"})
    Optional<Appointment> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"client", "client.user", "employee", "resource"})
    List<Appointment> findByClientIdOrderByScheduledStartDesc(Long clientId);

    @EntityGraph(attributePaths = {"client", "client.user", "employee", "resource"})
    List<Appointment> findByEmployeeIdOrderByScheduledStartAsc(Long employeeId);

    @EntityGraph(attributePaths = {"client", "client.user", "employee", "resource"})
    List<Appointment> findAllByOrderByScheduledStartDesc();

    @Query("""
            select (count(a) > 0) from Appointment a
            where a.employee.id = :employeeId
              and a.status in :statuses
              and a.scheduledStart < :end
              and a.scheduledEnd > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    boolean existsEmployeeOverlap(@Param("employeeId") Long employeeId,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end,
                                  @Param("statuses") Collection<AppointmentStatus> statuses,
                                  @Param("excludeId") Long excludeId);

    @Query("""
            select (count(a) > 0) from Appointment a
            where a.resource.id = :resourceId
              and a.status in :statuses
              and a.scheduledStart < :end
              and a.scheduledEnd > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    boolean existsResourceOverlap(@Param("resourceId") Long resourceId,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end,
                                  @Param("statuses") Collection<AppointmentStatus> statuses,
                                  @Param("excludeId") Long excludeId);

    @Query("""
            select a from Appointment a
            where a.employee.id = :employeeId
              and a.status in :statuses
              and a.scheduledStart < :end
              and a.scheduledEnd > :start
            """)
    List<Appointment> findEmployeeAppointmentsOverlapping(@Param("employeeId") Long employeeId,
                                                          @Param("start") Instant start,
                                                          @Param("end") Instant end,
                                                          @Param("statuses") Collection<AppointmentStatus> statuses);

    @Query("""
            select a from Appointment a
            where a.resource.id in :resourceIds
              and a.status in :statuses
              and a.scheduledStart < :end
              and a.scheduledEnd > :start
            """)
    List<Appointment> findResourceAppointmentsOverlapping(@Param("resourceIds") Collection<Long> resourceIds,
                                                          @Param("start") Instant start,
                                                          @Param("end") Instant end,
                                                          @Param("statuses") Collection<AppointmentStatus> statuses);

    long countByClientIdAndStatus(Long clientId, AppointmentStatus status);

    @Query("""
            select coalesce(sum(a.totalAmount), 0) from Appointment a
            where a.client.id = :clientId
              and a.status = :status
              and a.scheduledStart >= :since
            """)
    BigDecimal sumTotalAmountByClientSince(@Param("clientId") Long clientId,
                                           @Param("status") AppointmentStatus status,
                                           @Param("since") Instant since);
}
