package pl.servicedesk.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import pl.servicedesk.common.error.SlotUnavailableException;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.scheduling.service.AppointmentBookingService;
import pl.servicedesk.scheduling.service.AppointmentBookingService.BookCommand;
import pl.servicedesk.scheduling.service.AppointmentBookingService.ServiceLine;
import pl.servicedesk.support.AbstractIntegrationTest;

class ConcurrentBookingIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret";

    @Autowired
    private AppointmentBookingService bookingService;

    @Test
    void twoClientsRacingForTheSameSlotResultInExactlyOneBooking() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);
        String uid = Long.toHexString(System.nanoTime());

        String employeeEmail = "race-provider-" + uid + "@staff.local";
        long employeeId = onboardEmployee(admin, employeeEmail);
        String employeeToken = loginAndGetBearer(employeeEmail, PASSWORD);

        long categoryId = createCategory(admin, "Race " + uid);
        String serviceCode = "RACE-" + uid;
        createService(admin, categoryId, serviceCode);
        assignSkill(admin, employeeId, serviceCode);

        ZonedDateTime target = ZonedDateTime.now(ZoneId.of("Europe/Warsaw"))
                .plusDays(5).withHour(11).withMinute(0).withSecond(0).withNano(0);
        setWorkingHours(employeeToken, target.getDayOfWeek().name());

        long firstClientUserId = registerClientReturningUserId("race-a-" + uid + "@example.com");
        long secondClientUserId = registerClientReturningUserId("race-b-" + uid + "@example.com");

        BookCommand firstCommand = command(firstClientUserId, employeeId, serviceCode, target.toInstant());
        BookCommand secondCommand = command(secondClientUserId, employeeId, serviceCode, target.toInstant());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Callable<Outcome> first = attempt(firstCommand, ready, go);
        Callable<Outcome> second = attempt(secondCommand, ready, go);
        Future<Outcome> firstResult = pool.submit(first);
        Future<Outcome> secondResult = pool.submit(second);

        ready.await(5, TimeUnit.SECONDS);
        go.countDown();

        List<Outcome> outcomes = List.of(
                firstResult.get(20, TimeUnit.SECONDS),
                secondResult.get(20, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertThat(outcomes).containsExactlyInAnyOrder(Outcome.BOOKED, Outcome.REJECTED);
    }

    private Callable<Outcome> attempt(BookCommand command, CountDownLatch ready, CountDownLatch go) {
        return () -> {
            ready.countDown();
            go.await();
            try {
                bookingService.book(command);
                return Outcome.BOOKED;
            } catch (SlotUnavailableException | DataAccessException expected) {
                return Outcome.REJECTED;
            }
        };
    }

    private BookCommand command(long clientUserId, long employeeId, String serviceCode, Instant start) {
        return new BookCommand(clientUserId, employeeId, List.of(new ServiceLine(serviceCode, 1)), start, null);
    }

    private long onboardEmployee(String admin, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/employees")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","firstName":"Race","lastName":"Provider",
                                 "displayName":"Race Provider","title":"Therapist","hiredOn":"2025-01-02"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createCategory(String admin, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/service-categories")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","displayOrder":1}""".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void createService(String admin, long categoryId, String code) throws Exception {
        mockMvc.perform(post("/api/v1/admin/services")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"code":"%s","name":"Race service",
                                 "basePrice":100.00,"durationMinutes":60,"bufferMinutes":0}
                                """.formatted(categoryId, code)))
                .andExpect(status().isCreated());
    }

    private void assignSkill(String admin, long employeeId, String serviceCode) throws Exception {
        mockMvc.perform(put("/api/v1/admin/employees/" + employeeId + "/skills")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceCodes":["%s"]}""".formatted(serviceCode)))
                .andExpect(status().isOk());
    }

    private void setWorkingHours(String employeeToken, String day) throws Exception {
        mockMvc.perform(put("/api/v1/employees/me/working-hours")
                        .header(HttpHeaders.AUTHORIZATION, employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shifts":[{"dayOfWeek":"%s","startTime":"08:00","endTime":"18:00"}]}
                                """.formatted(day)))
                .andExpect(status().isOk());
    }

    private long registerClientReturningUserId(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", email, "password", PASSWORD,
                                "firstName", "Race", "lastName", "Client", "marketingConsent", false))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("userId").asLong();
    }

    private enum Outcome {
        BOOKED,
        REJECTED
    }
}
