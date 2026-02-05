package api.m2.movements;
import api.m2.movements.configuration.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class})
public class MovementsApplication {
    static void main(String[] args) {
        SpringApplication.run(MovementsApplication.class, args);
    }
}
