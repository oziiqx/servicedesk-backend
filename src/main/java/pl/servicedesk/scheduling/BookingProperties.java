package pl.servicedesk.scheduling;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "servicedesk.booking")
public record BookingProperties(
        @NotBlank String zone,
        @NotNull Duration minLeadTime,
        @NotNull Period maxAdvanceBooking,
        @NotNull Duration lateCancellationWindow,
        @NotNull Duration slotGranularity) {

    public ZoneId zoneId() {
        return ZoneId.of(zone);
    }
}
