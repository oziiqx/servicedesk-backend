package pl.servicedesk.clients;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import pl.servicedesk.identity.domain.RoleName;
import pl.servicedesk.support.AbstractIntegrationTest;

class ClientProfileIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sup3rSecret";

    @Test
    void aFreshClientStartsAtTheEntryTierWithNoHistory() throws Exception {
        String email = "profile-" + Long.toHexString(System.nanoTime()) + "@example.com";
        registerClient(email, PASSWORD);
        String client = loginAndGetBearer(email, PASSWORD);

        mockMvc.perform(get("/api/v1/clients/me").header(HttpHeaders.AUTHORIZATION, client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Test Client"))
                .andExpect(jsonPath("$.loyaltyTier").value("BRONZE"))
                .andExpect(jsonPath("$.completedAppointments").value(0));

        mockMvc.perform(get("/api/v1/clients/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(RoleName.EMPLOYEE)))
                .andExpect(status().isForbidden());
    }
}
