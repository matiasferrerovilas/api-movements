package api.expenses.expenses.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import spock.util.time.MutableClock

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@TestConfiguration
class ClockConfig {
    protected final static String TODAY_ARGENTINA_MOCK = "2025-10-03"
    protected final static String TODAY_ARGENTINA_DATETIME_MOCK = "${TODAY_ARGENTINA_MOCK}T23:00:00.00-03:00"
    public final static Instant INSTANT_ARGENTINA_MOCK = Instant.parse(TODAY_ARGENTINA_DATETIME_MOCK)

    @Bean
    @Primary
    Clock clock() {
        return new MutableClock(INSTANT_ARGENTINA_MOCK, ZoneId.of("UTC"))
    }
}

