package pl.servicedesk.identity.bootstrap;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "servicedesk.bootstrap")
public record BootstrapProperties(
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 8) String adminPassword,
        @NotBlank String adminFirstName,
        @NotBlank String adminLastName) {

    static final String DEFAULT_PASSWORD = "ChangeMe!123";

    boolean usesDefaultPassword() {
        return DEFAULT_PASSWORD.equals(adminPassword);
    }
}
