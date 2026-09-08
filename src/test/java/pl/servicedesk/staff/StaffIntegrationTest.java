package pl.servicedesk.staff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.support.AbstractIntegrationTest;

class StaffIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret";

    @Test
    void onboardedEmployeeManagesOwnScheduleAndTimeOff() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);

        String email = "clara-" + uid() + "@staff.local";
        long employeeId = onboard(admin, email);
        String employee = loginAndGetBearer(email, PASSWORD);

        mockMvc.perform(put("/api/v1/employees/me/working-hours")
                        .header(HttpHeaders.AUTHORIZATION, employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shifts":[
                                  {"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"17:00"},
                                  {"dayOfWeek":"TUESDAY","startTime":"09:00","endTime":"13:00"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[1].dayOfWeek").value("TUESDAY"));

        mockMvc.perform(put("/api/v1/employees/me/working-hours")
                        .header(HttpHeaders.AUTHORIZATION, employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shifts":[
                                  {"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00"},
                                  {"dayOfWeek":"MONDAY","startTime":"13:00","endTime":"17:00"}]}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://servicedesk.pl/problems/business-rule-violation"));

        mockMvc.perform(post("/api/v1/employees/me/time-off")
                        .header(HttpHeaders.AUTHORIZATION, employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt":"2027-06-01T08:00:00Z","endsAt":"2027-06-08T16:00:00Z","reason":"Holiday"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("Holiday"));

        mockMvc.perform(post("/api/v1/employees/me/time-off")
                        .header(HttpHeaders.AUTHORIZATION, employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt":"2020-01-01T00:00:00Z","endsAt":"2020-01-05T00:00:00Z"}"""))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/v1/admin/employees/" + employeeId)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void adminAssignsSkillsThatShowUpInTheDirectory() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);
        long employeeId = onboard(admin, "mark-" + uid() + "@staff.local");
        long categoryId = createCategory(admin, "Diagnostics " + uid());
        String serviceCode = createService(admin, categoryId, "DIAG-" + uid());

        mockMvc.perform(put("/api/v1/admin/employees/" + employeeId + "/skills")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceCodes":["%s"]}""".formatted(serviceCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceCode").value(serviceCode))
                .andExpect(jsonPath("$[0].serviceName").value("Standard diagnostics"));

        mockMvc.perform(put("/api/v1/admin/employees/" + employeeId + "/skills")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceCodes":["%s","GHOST"]}""".formatted(serviceCode)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/skills")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(RoleName.CLIENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceCode").value(serviceCode));
    }

    @Test
    void staffEndpointsEnforceRoles() throws Exception {
        mockMvc.perform(post("/api/v1/admin/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(RoleName.CLIENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardPayload("blocked@staff.local")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/employees/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(RoleName.CLIENT)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(RoleName.CLIENT)))
                .andExpect(status().isOk());
    }

    private long onboard(String adminToken, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/employees")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onboardPayload(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String onboardPayload(String email) {
        return """
                {"email":"%s","password":"%s","firstName":"Staff","lastName":"Member",
                 "displayName":"Staff Member","title":"Specialist","hiredOn":"2025-01-02"}
                """.formatted(email, PASSWORD);
    }

    private long createCategory(String adminToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/service-categories")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","displayOrder":1}""".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private String createService(String adminToken, long categoryId, String code) throws Exception {
        mockMvc.perform(post("/api/v1/admin/services")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"code":"%s","name":"Standard diagnostics",
                                 "basePrice":149.99,"durationMinutes":45,"bufferMinutes":15}
                                """.formatted(categoryId, code)))
                .andExpect(status().isCreated());
        return code;
    }

    private static String uid() {
        return Long.toHexString(System.nanoTime());
    }
}
