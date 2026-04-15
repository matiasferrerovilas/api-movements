package api.m2.movements.integration

import api.m2.movements.entities.Category
import api.m2.movements.entities.Currency
import api.m2.movements.entities.Movement
import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.entities.WorkspaceMember
import api.m2.movements.enums.MovementType
import api.m2.movements.enums.UserType
import api.m2.movements.enums.WorkspaceRole
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.repositories.MembershipRepository
import api.m2.movements.repositories.MovementRepository
import api.m2.movements.repositories.UserRepository
import api.m2.movements.repositories.WorkspaceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import java.time.LocalDate

@SpringBootTest
@Testcontainers
@Transactional
class MovementRepositoryIntegrationTest extends Specification {

    @Container
    @Shared
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

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
    }

    @Autowired
    MovementRepository movementRepository

    @Autowired
    UserRepository userRepository

    @Autowired
    WorkspaceRepository workspaceRepository

    @Autowired
    MembershipRepository membershipRepository

    @Autowired
    CategoryRepository categoryRepository

    @Autowired
    CurrencyRepository currencyRepository

    User user
    Workspace workspace
    Currency ars
    Category category

    def setup() {
        user = userRepository.save(User.builder()
                .email("test@test.com")
                .isFirstLogin(false)
                .userType(UserType.PERSONAL)
                .build())

        workspace = workspaceRepository.save(Workspace.builder()
                .name("Test Workspace")
                .owner(user)
                .build())

        membershipRepository.save(WorkspaceMember.builder()
                .user(user)
                .workspace(workspace)
                .role(WorkspaceRole.OWNER)
                .build())

        ars = currencyRepository.findBySymbol("ARS")
                .orElseGet { currencyRepository.save(Currency.builder().description("Peso Argentino").symbol("ARS").enabled(true).build()) }

        category = categoryRepository.findAll().first()
    }

    def "getBalanceByFilters - should return sum of movements only for the given user"() {
        given:
        def otherUser = userRepository.save(User.builder().email("other@test.com").isFirstLogin(false).userType(UserType.PERSONAL).build())
        def otherWorkspace = workspaceRepository.save(Workspace.builder().name("Other Workspace").owner(otherUser).build())

        def startDate = LocalDate.now().minusDays(30)
        def endDate = LocalDate.now().plusDays(1)

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("1000.00"))
                .description("Gasto usuario correcto")
                .type(MovementType.DEBITO)
                .date(LocalDate.now())
                .owner(user)
                .workspace(workspace)
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
                .owner(otherUser)
                .workspace(otherWorkspace)
                .currency(ars)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = movementRepository.getBalanceByFilters(
                startDate,
                endDate,
                "test@test.com",
                [MovementType.DEBITO.name()],
                [workspace.id as Integer],
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
                "test@test.com",
                [MovementType.DEBITO.name()],
                [workspace.id as Integer],
                [ars.id as Integer]
        )

        then:
        result == BigDecimal.ZERO
    }

    def "getBalanceWithCategoryByYear - should return category totals for user only, excluding other users"() {
        given:
        def otherUser = userRepository.save(User.builder().email("other2@test.com").isFirstLogin(false).userType(UserType.PERSONAL).build())
        def otherWorkspace = workspaceRepository.save(Workspace.builder().name("Other2 Workspace").owner(otherUser).build())

        def now = LocalDate.now()

        movementRepository.save(Movement.builder()
                .amount(new BigDecimal("300.00"))
                .description("Comida test user")
                .type(MovementType.DEBITO)
                .date(now)
                .owner(user)
                .workspace(workspace)
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
                .owner(otherUser)
                .workspace(otherWorkspace)
                .currency(ars)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = movementRepository.getBalanceWithCategoryByYear(
                now.year,
                now.monthValue,
                [workspace.id as Integer],
                ["ARS"],
                "test@test.com"
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
                .owner(user)
                .workspace(workspace)
                .currency(ars)
                .category(category)
                .cuotaActual(0)
                .cuotasTotales(0)
                .build())

        when:
        def result = movementRepository.getBalanceByYearAndGroup(
                now.year,
                now.monthValue,
                "test@test.com"
        )

        then:
        result.size() == 1
        def row = result.first()
        row.workspaceDescription == "Test Workspace"
        row.total == new BigDecimal("400.00")
        row.currencySymbol == "ARS"
    }
}
