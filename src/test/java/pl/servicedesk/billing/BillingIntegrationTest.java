package pl.servicedesk.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.support.AbstractIntegrationTest;

class BillingIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret";
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    @Test
    void completingAnAppointmentIssuesAnInvoiceThatCanBePaidOff() throws Exception {
        Fixture fixture = fixture();
        String clientEmail = "billed-" + Long.toHexString(System.nanoTime()) + "@example.com";
        registerClient(clientEmail, PASSWORD);
        String client = loginAndGetBearer(clientEmail, PASSWORD);

        long appointmentId = book(client, fixture);
        complete(fixture.employeeToken(), appointmentId);

        MvcResult invoices = mockMvc.perform(get("/api/v1/invoices").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ISSUED"))
                .andExpect(jsonPath("$[0].netAmount").value(200.00))
                .andExpect(jsonPath("$[0].taxAmount").value(46.00))
                .andExpect(jsonPath("$[0].grossAmount").value(246.00))
                .andExpect(jsonPath("$[0].invoiceNumber").value(org.hamcrest.Matchers.startsWith("INV-")))
                .andReturn();
        long invoiceId = objectMapper.readTree(invoices.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        String admin = bearerTokenFor(RoleName.ADMIN);

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100.00,"method":"CARD","reference":"auth-1"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.paidAmount").value(100.00));

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":146.00,"method":"TRANSFER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payments.length()").value(2));
    }

    @Test
    void invoiceAccessIsRestrictedToTheOwningClientAndAdmins() throws Exception {
        Fixture fixture = fixture();
        String ownerEmail = "owner-" + Long.toHexString(System.nanoTime()) + "@example.com";
        registerClient(ownerEmail, PASSWORD);
        String owner = loginAndGetBearer(ownerEmail, PASSWORD);

        long appointmentId = book(owner, fixture);
        complete(fixture.employeeToken(), appointmentId);

        MvcResult invoices = mockMvc.perform(get("/api/v1/invoices").header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andReturn();
        long invoiceId = objectMapper.readTree(invoices.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        String otherEmail = "intruder-" + Long.toHexString(System.nanoTime()) + "@example.com";
        registerClient(otherEmail, PASSWORD);
        String intruder = loginAndGetBearer(otherEmail, PASSWORD);

        mockMvc.perform(get("/api/v1/invoices/" + invoiceId).header(HttpHeaders.AUTHORIZATION, intruder))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"method":"CASH"}"""))
                .andExpect(status().isForbidden());
    }

    private long book(String clientToken, Fixture fixture) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeId":%d,"services":[{"serviceCode":"%s","quantity":1}],"scheduledStart":"%s"}
                                """.formatted(fixture.employeeId(), fixture.serviceCode(), fixture.start())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void complete(String employeeToken, long appointmentId) throws Exception {
        for (String target : new String[] {"CONFIRMED", "IN_PROGRESS", "COMPLETED"}) {
            mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/transition")
                            .header(HttpHeaders.AUTHORIZATION, employeeToken)
                            .param("status", target))
                    .andExpect(status().isOk());
        }
    }

    private Fixture fixture() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);
        String uid = Long.toHexString(System.nanoTime());

        String employeeEmail = "billing-provider-" + uid + "@staff.local";
        long employeeId = onboardEmployee(admin, employeeEmail);
        String employeeToken = loginAndGetBearer(employeeEmail, PASSWORD);

        long categoryId = createCategory(admin, "Billing " + uid);
        String serviceCode = "BILLED-" + uid;
        createService(admin, categoryId, serviceCode);
        assignSkill(admin, employeeId, serviceCode);

        ZonedDateTime target = ZonedDateTime.now(ZONE)
                .plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        while (target.getDayOfWeek() == DayOfWeek.SATURDAY || target.getDayOfWeek() == DayOfWeek.SUNDAY) {
            target = target.plusDays(1);
        }
        setWorkingHours(employeeToken, target.getDayOfWeek());
        return new Fixture(employeeId, employeeToken, serviceCode, target.toInstant());
    }

    private long onboardEmployee(String admin, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/employees")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","firstName":"Bill","lastName":"Provider",
                                 "displayName":"Bill Provider","title":"Therapist","hiredOn":"2025-01-02"}
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
                                {"categoryId":%d,"code":"%s","name":"Billed service",
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

    private record Fixture(long employeeId, String employeeToken, String serviceCode, Instant start) {
    }
}
