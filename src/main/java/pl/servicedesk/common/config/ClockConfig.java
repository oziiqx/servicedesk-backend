package pl.servicedesk.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
