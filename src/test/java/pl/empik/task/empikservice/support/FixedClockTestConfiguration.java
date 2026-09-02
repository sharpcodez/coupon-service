package pl.empik.task.empikservice.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@TestConfiguration(proxyBeanMethods = false)
public class FixedClockTestConfiguration {

    public static final Instant FIXED_NOW = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    }
}
