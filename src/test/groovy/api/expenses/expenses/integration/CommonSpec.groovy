package api.expenses.expenses.integration

import api.expenses.expenses.config.ClockConfig
import api.expenses.expenses.config.LiquibaseConfig
import groovy.json.JsonSlurper
import org.spockframework.spring.EnableSharedInjection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import java.time.Clock
import java.time.ZoneId

@Testcontainers
@SpringBootTest(classes= [ ClockConfig, LiquibaseConfig])
@EnableSharedInjection
class CommonSpec extends Specification {


    @Autowired
    @Shared
    Clock clock

    @Shared
    def testUser = "test@example.com"

    JsonSlurper jsonSlurper = new JsonSlurper()

    @Container
    @Shared
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.43")
            .withDatabaseName("expenses_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        mysql.start()
        registry.add("spring.datasource.url", mysql::getJdbcUrl)
        registry.add("spring.datasource.username", mysql::getUsername)
        registry.add("spring.datasource.password", mysql::getPassword)
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName)
        registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
    }

    def setup() {
        clock.getZone() >> ZoneId.of("UTC")
        clock.setInstant(ClockConfig.INSTANT_ARGENTINA_MOCK)
        authenticateUser(testUser)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }


    private static void authenticateUser(String email) {
        def auth = new UsernamePasswordAuthenticationToken(email, null, [])
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext())
        SecurityContextHolder.context.authentication = auth
    }
}
