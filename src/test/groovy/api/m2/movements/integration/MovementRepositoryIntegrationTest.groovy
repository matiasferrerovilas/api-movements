package api.m2.movements.integration

import api.m2.movements.movements.entities.commons.Category
import api.m2.movements.movements.entities.commons.Currency
import api.m2.movements.movements.entities.movements.Movement

import api.m2.movements.movements.enums.MovementType
import api.m2.movements.movements.repositories.CategoryRepository
import api.m2.movements.movements.repositories.CurrencyRepository
import api.m2.movements.movements.repositories.MovementRepository

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.LocalDate

@SpringBootTest
@Testcontainers
@Transactional
class MovementRepositoryIntegrationTest extends Specification {

    // Contexto completo: IdentityClientConfig crea el bean real de IdentityClient.
    // En vez de mockear la interfaz, se levanta un WireMockServer y se apunta
    // identity.base-url ahí (nada en este test se autentica, así que no hace
    // falta registrar stubs: no se espera ningún request real).
    @Shared
    static WireMockServer identityMock

    static {
        identityMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        identityMock.start()
    }

    @Container
    @Shared
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .withStartupTimeout(Duration.ofMinutes(5))

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> "true")
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/changelog.yaml")
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect")
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                      "org.springframework.boot.autoconfigure.amqp.RabbitStreamTemplateAutoConfiguration," +
                      "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration")
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "https://test-issuer.local/realms/test")
        registry.add("keycloak.auth-server-url", () -> "https://test-issuer.local")
        registry.add("identity.base-url", () -> "http://localhost:${identityMock.port()}".toString())
    }

    @Autowired
    MovementRepository movementRepository

    @Autowired
    CategoryRepository categoryRepository

    @Autowired
    CurrencyRepository currencyRepository

    Long userId = 1L
    Long workspaceId = 1L
    Currency ars
    Category category

    def setup() {
        ars = currencyRepository.findBySymbol("ARS")
                .orElseGet { currencyRepository.save(Currency.builder().description("Peso Argentino").symbol("ARS").enabled(true).build()) }

        category = categoryRepository.findAll().first()
    }

    def "getBalanceByFilters - should return sum of movements only for the given user"() {
        given:
        def otherUserId = 2L
        def otherWorkspaceId = 2L

        def startDate = LocalDate.now().minusDays(30)
        def endDate = LocalDate.now().plusDays(1)

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("1000.00"))
                .description("Gasto usuario correcto")
                .type(MovementType.DEBITO)
                .date(LocalDate.now())
                .ownerId(userId)
                .workspaceId(workspaceId)
                .currency(ars)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("500.00"))
                .description("Gasto de otro usuario - no debe sumarse")
                .type(MovementType.DEBITO)
                .date(LocalDate.now())
                .ownerId(otherUserId)
                .workspaceId(otherWorkspaceId)
                .currency(ars)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = movementRepository.getBalanceByFilters(
                startDate,
                endDate,
                userId,
                [MovementType.DEBITO.name()],
                [workspaceId as Integer],
                [ars.id as Integer]
        )

        then:
        result == new BigDecimal("1000.00")
    }

    def "getBalanceByFilters - should return zero when no matching movements"() {
        given:
        def startDate = LocalDate.now().minusDays(30)
        def endDate = LocalDate.now().plusDays(1)

        when:
        def result = movementRepository.getBalanceByFilters(
                startDate,
                endDate,
                userId,
                [MovementType.DEBITO.name()],
                [workspaceId as Integer],
                [ars.id as Integer]
        )

        then:
        result == BigDecimal.ZERO
    }

    def "getBalanceWithCategoryByYear - should return category totals for user only, excluding other users"() {
        given:
        def otherUserId = 3L
        def otherWorkspaceId = 3L

        def now = LocalDate.now()

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("300.00"))
                .description("Comida test user")
                .type(MovementType.DEBITO)
                .date(now)
                .ownerId(userId)
                .workspaceId(workspaceId)
                .currency(ars)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("999.00"))
                .description("Comida otro usuario - no debe aparecer")
                .type(MovementType.DEBITO)
                .date(now)
                .ownerId(otherUserId)
                .workspaceId(otherWorkspaceId)
                .currency(ars)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = movementRepository.getBalanceWithCategoryByYear(
                now.year,
                now.monthValue,
                [workspaceId as Integer],
                ["ARS"]
        )

        then:
        result.size() == 1
        def row = result.first()
        row.total == new BigDecimal("300.00")
        row.currencySymbol == "ARS"
    }

    def "getBalanceByYearAndGroup - should return totals grouped by workspace for user"() {
        given:
        def now = LocalDate.now()

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("400.00"))
                .description("Gasto grupo")
                .type(MovementType.DEBITO)
                .date(now)
                .ownerId(userId)
                .workspaceId(workspaceId)
                .currency(ars)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = movementRepository.getBalanceByYearAndGroup(
                now.year,
                now.monthValue,
                userId
        )

        then:
        result.size() == 1
        def row = result.first()
        row.workspaceId == workspaceId
        row.total == new BigDecimal("400.00")
        row.currencySymbol == "ARS"
    }
}
