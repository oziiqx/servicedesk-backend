package pl.servicedesk.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import pl.servicedesk.support.AbstractIntegrationTest;

class CorsIntegrationTest extends AbstractIntegrationTest {

    @Test
    void allowsPreflightFromTheConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/services")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")));
    }

    @Test
    void rejectsPreflightFromAnUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/services")
                        .header("Origin", "http://not-allowed.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
