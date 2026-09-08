package pl.servicedesk.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.support.AbstractIntegrationTest;

class CatalogIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminManagesCatalogWhileClientsOnlyRead() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);
        String client = bearerTokenFor(RoleName.CLIENT);

        mockMvc.perform(post("/api/v1/admin/service-categories")
                        .header(HttpHeaders.AUTHORIZATION, client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Diagnostics","description":"Inspections","displayOrder":1}"""))
                .andExpect(status().isForbidden());

        long categoryId = createCategory(admin, "Diagnostics", 1);

        mockMvc.perform(post("/api/v1/admin/services")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"code":"DIAG-STD","name":"Standard diagnostics",
                                 "description":"45 minute inspection","basePrice":149.99,
                                 "durationMinutes":45,"bufferMinutes":15,"requiredResourceType":"STATION"}
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DIAG-STD"))
                .andExpect(jsonPath("$.categoryName").value("Diagnostics"))
                .andExpect(jsonPath("$.basePrice").value(149.99))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/services/DIAG-STD").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredResourceType").value("STATION"));

        mockMvc.perform(get("/api/v1/services").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'DIAG-STD')]").exists());

        mockMvc.perform(post("/api/v1/admin/services")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"code":"DIAG-STD","name":"Duplicate",
                                 "basePrice":10.00,"durationMinutes":30,"bufferMinutes":0}
                                """.formatted(categoryId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://servicedesk.pl/problems/duplicate-resource"));
    }

    @Test
    void rejectsInvalidServicePayload() throws Exception {
        String admin = bearerTokenFor(RoleName.ADMIN);
        long categoryId = createCategory(admin, "Repairs", 2);

        mockMvc.perform(post("/api/v1/admin/services")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":%d,"code":"","name":"Broken","basePrice":-5,
                                 "durationMinutes":0,"bufferMinutes":-1}
                                """.formatted(categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        org.hamcrest.Matchers.hasItems("code", "basePrice", "durationMinutes", "bufferMinutes")));
    }

    @Test
    void clientCannotBrowseResources() throws Exception {
        mockMvc.perform(get("/api/v1/resources").header(HttpHeaders.AUTHORIZATION, bearerTokenFor(RoleName.CLIENT)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/resources").header(HttpHeaders.AUTHORIZATION, bearerTokenFor(RoleName.EMPLOYEE)))
                .andExpect(status().isOk());
    }

    private long createCategory(String adminToken, String name, int displayOrder) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/service-categories")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","displayOrder":%d}""".formatted(name, displayOrder)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }
}
