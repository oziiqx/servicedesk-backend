package pl.servicedesk.pricing;

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

class PricingIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret";
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    @Test
    void weekendSurchargeIsAppliedWhenBookingASaturdaySlot() throws Exception {
        Fixture fixture = fixture();
        String client = registerAndLoginClient();

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(fixture, fixture.slotAt(10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netAmount").value(200.00))
                .andExpect(jsonPath("$.surchargeAmount").value(30.00))
                .andExpect(jsonPath("$.discountAmount").value(0.00))
                .andExpect(jsonPath("$.totalAmount").value(230.00))
                .andExpect(jsonPath("$.loyaltyTierAtBooking").value("BRONZE"));
    }

    @Test
    void loyaltyDiscountAppliesOnceEnoughAppointmentsAreCompleted() throws Exception {
        Fixture fixture = fixture();
        String clientEmail = "loyal-" + Long.toHexString(System.nanoTime()) + "@example.com";
        registerClient(clientEmail, PASSWORD);
        String client = loginAndGetBearer(clientEmail, PASSWORD);

        for (int hour : new int[] {8, 10, 12}) {
            long appointmentId = book(client, fixture, fixture.slotAt(hour));
            complete(fixture.employeeToken(), appointmentId);
        }

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(fixture, fixture.slotAt(14))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loyaltyTierAtBooking").value("SILVER"))
                .andExpect(jsonPath("$.discountPercentage").value(5.00))
                .andExpect(jsonPath("$.discountAmount").value(11.50))
                .andExpect(jsonPath("$.totalAmount").value(218.50));
    }

    @Test
    void adminManagesLoyaltyTiersAndPricingRules() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);
        String uid = Long.toHexString(System.nanoTime());

        mockMvc.perform(get("/api/v1/admin/loyalty-tiers").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'GOLD')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'BRONZE')]").exists());

        MvcResult created = mockMvc.perform(post("/api/v1/admin/loyalty-tiers")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"PLATINUM-%s","minCompletedAppointments":25,"minLifetimeSpend":5000.00,
                                 "discountPercentage":15.00,"displayOrder":4}""".formatted(uid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountPercentage").value(15.00))
                .andReturn();
        long tierId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/loyalty-tiers/" + tierId)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minCompletedAppointments":30,"minLifetimeSpend":6000.00,
                                 "discountPercentage":18.00,"displayOrder":4}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountPercentage").value(18.00));

        mockMvc.perform(post("/api/v1/admin/pricing-rules")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"EARLY-BIRD-%s","description":"Morning discount","adjustmentPercentage":-10.00,
                                 "daysOfWeek":"1,2,3,4,5","timeFrom":"07:00","timeTo":"09:00","priority":5}""".formatted(uid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));
    }

    private long book(String clientToken, Fixture fixture, Instant slot) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(fixture, slot)))
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

    private String bookingPayload(Fixture fixture, Instant start) {
        return """
                {"employeeId":%d,"services":[{"serviceCode":"%s","quantity":1}],"scheduledStart":"%s"}
                """.formatted(fixture.employeeId(), fixture.serviceCode(), start);
    }

    private Fixture fixture() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);
        String uid = Long.toHexString(System.nanoTime());

        String employeeEmail = "pricing-provider-" + uid + "@staff.local";
        long employeeId = onboardEmployee(admin, employeeEmail);
        String employeeToken = loginAndGetBearer(employeeEmail, PASSWORD);

        long categoryId = createCategory(admin, "Pricing " + uid);
        String serviceCode = "PRICED-" + uid;
        createService(admin, categoryId, serviceCode);
        assignSkill(admin, employeeId, serviceCode);

        ZonedDateTime saturday = ZonedDateTime.now(ZONE).plusDays(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        while (saturday.getDayOfWeek() != DayOfWeek.SATURDAY) {
            saturday = saturday.plusDays(1);
        }
        setWorkingHours(employeeToken, DayOfWeek.SATURDAY);
        return new Fixture(employeeId, employeeToken, serviceCode, saturday.toLocalDate());
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
                                {"categoryId":%d,"code":"%s","name":"Priced service",
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
                                {"shifts":[{"dayOfWeek":"%s","startTime":"07:00","endTime":"18:00"}]}
                                """.formatted(day.name())))
                .andExpect(status().isOk());
    }

    private String registerAndLoginClient() throws Exception {
        String email = "pclient-" + Long.toHexString(System.nanoTime()) + "@example.com";
        registerClient(email, PASSWORD);
        return loginAndGetBearer(email, PASSWORD);
    }

    private record Fixture(long employeeId, String employeeToken, String serviceCode, LocalDate date) {
        Instant slotAt(int hour) {
            return ZonedDateTime.of(date, java.time.LocalTime.of(hour, 0), ZONE).toInstant();
        }
    }
}
