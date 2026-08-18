package api.m2.movements.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expone un {@link Clock} como bean para poder inyectarlo (y así poder
 * controlar la fecha/hora "actual" en tests) en servicios que dependen del
 * tiempo, en lugar de llamar directamente a {@code LocalDate.now()} / {@code YearMonth.now()}.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
