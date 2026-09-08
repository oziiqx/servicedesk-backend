package pl.servicedesk.scheduling;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.support.AbstractIntegrationTest;

class SchedulingIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret";
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    @Test
    void clientBooksAnAppointmentAndTheSlotCannotBeDoubleBooked() throws Exception {
        Fixture fixture = fixture();
        String client = registerAndLoginClient();

        MvcResult booked = mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(fixture)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.employeeId").value(fixture.employeeId()))
                .andExpect(jsonPath("$.totalAmount").value(200.00))
                .andExpect(jsonPath("$.items[0].serviceCode").value(fixture.serviceCode()))
                .andReturn();
        long appointmentId = readLong(booked, "id");

        String secondClient = registerAndLoginClient();
        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, secondClient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(fixture)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://servicedesk.pl/problems/slot-unavailable"));

        mockMvc.perform(get("/api/v1/availability")
                        .header(HttpHeaders.AUTHORIZATION, client)
                        .param("employeeId", String.valueOf(fixture.employeeId()))
                        .param("serviceCode", fixture.serviceCode())
                        .param("date", fixture.date().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.start == '%s')]".formatted(fixture.start())).doesNotExist());

        mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId));
    }

    @Test
    void assignedEmployeeMovesTheAppointmentThroughItsLifecycle() throws Exception {
        Fixture fixture = fixture();
        String client = registerAndLoginClient();

        MvcResult booked = mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(fixture)))
                .andExpect(status().isCreated())
                .andReturn();
        long appointmentId = readLong(booked, "id");

        transition(fixture.employeeToken(), appointmentId, "CONFIRMED").andExpect(status().isOk());
        transition(fixture.employeeToken(), appointmentId, "IN_PROGRESS").andExpect(status().isOk());
        transition(fixture.employeeToken(), appointmentId, "COMPLETED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        transition(fixture.employeeToken(), appointmentId, "PENDING")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://servicedesk.pl/problems/invalid-appointment-state"));
    }

    @Test
    void schedulingEndpointsEnforceRoles() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, fixture.employeeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(fixture)))
                .andExpect(status().isForbidden());

        String client = registerAndLoginClient();
        MvcResult booked = mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(fixture)))
                .andExpect(status().isCreated())
                .andReturn();
        long appointmentId = readLong(booked, "id");

        transition(client, appointmentId, "CONFIRMED").andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Plans changed"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private org.springframework.test.web.servlet.ResultActions transition(String token, long appointmentId,
                                                                          String status) throws Exception {
        return mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/transition")
                .header(HttpHeaders.AUTHORIZATION, token)
                .param("status", status));
    }

    private String bookingPayload(Fixture fixture) {
        return """
                {"employeeId":%d,"services":[{"serviceCode":"%s","quantity":1}],"scheduledStart":"%s"}
                """.formatted(fixture.employeeId(), fixture.serviceCode(), fixture.start());
    }

    private Fixture fixture() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);
        String uid = Long.toHexString(System.nanoTime());

        String employeeEmail = "provider-" + uid + "@staff.local";
        long employeeId = onboardEmployee(admin, employeeEmail);
        String employeeToken = loginAndGetBearer(employeeEmail, PASSWORD);

        long categoryId = createCategory(admin, "Wellness " + uid);
        String serviceCode = "MASSAGE-" + uid;
        createService(admin, categoryId, serviceCode);
        assignSkill(admin, employeeId, serviceCode);

        ZonedDateTime target = ZonedDateTime.now(ZONE)
                .plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0);
        while (target.getDayOfWeek() == DayOfWeek.SATURDAY || target.getDayOfWeek() == DayOfWeek.SUNDAY) {
            target = target.plusDays(1);
        }
        setWorkingHours(employeeToken, target.getDayOfWeek());

        return new Fixture(employeeId, employeeToken, serviceCode, target.toInstant(), target.toLocalDate());
    }

    private long onboardEmployee(String admin, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/employees")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","firstName":"Pat","lastName":"Provider",
                                 "displayName":"Pat Provider","title":"Therapist","hiredOn":"2025-01-02"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return readLong(result, "id");
    }

    private long createCategory(String admin, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/service-categories")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","displayOrder":1}""".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return readLong(result, "id");
    }

    private void createService(String admin, long categoryId, String code) throws Exception {
        mockMvc.perform(post("/api/v1/admin/services")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"code":"%s","name":"Deep tissue massage",
                                 "basePrice":200.00,"durationMinutes":60,"bufferMinutes":0}
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

    private void setWorkingHours(String employeeToken, DayOfWeek day) throws Exception {
        mockMvc.perform(put("/api/v1/employees/me/working-hours")
                        .header(HttpHeaders.AUTHORIZATION, employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shifts":[{"dayOfWeek":"%s","startTime":"08:00","endTime":"18:00"}]}
                                """.formatted(day.name())))
                .andExpect(status().isOk());
    }

    private String registerAndLoginClient() throws Exception {
        String email = "client-" + Long.toHexString(System.nanoTime()) + "@example.com";
        registerClient(email, PASSWORD);
        return loginAndGetBearer(email, PASSWORD);
    }

    private long readLong(MvcResult result, String field) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get(field).asLong();
    }

    private record Fixture(long employeeId, String employeeToken, String serviceCode, Instant start, LocalDate date) {
    }
}
