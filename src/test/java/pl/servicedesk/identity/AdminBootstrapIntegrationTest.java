package pl.servicedesk.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import pl.servicedesk.support.AbstractIntegrationTest;

class AdminBootstrapIntegrationTest extends AbstractIntegrationTest {

    @Test
    void aBootstrapAdminAccountIsUsableAfterStartup() throws Exception {
        String admin = loginAndGetBearer("admin@servicedesk.local", "ChangeMe!123");

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@servicedesk.local"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }
}
