package api.m2.movements;
import api.m2.movements.configuration.properties.CorsProperties;
import api.m2.movements.configuration.properties.JwtProperties;
import dev.nativehint.NativeHint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class})
@NativeHint
public class MovementsApplication {
    static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(MovementsApplication.class, args);
    }
}
