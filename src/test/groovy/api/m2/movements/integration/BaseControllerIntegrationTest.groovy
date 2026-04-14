package api.m2.movements.integration

import api.m2.movements.entities.Category
import api.m2.movements.entities.Currency
import api.m2.movements.entities.User
import api.m2.movements.entities.UserSetting
import api.m2.movements.entities.Workspace
import api.m2.movements.entities.WorkspaceMember
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.enums.WorkspaceRole
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.repositories.CurrencyRepository
import api.m2.movements.repositories.MembershipRepository
import api.m2.movements.repositories.UserRepository
import api.m2.movements.repositories.UserSettingRepository
import api.m2.movements.repositories.WorkspaceRepository
import api.m2.movements.services.currencies.ExchangeRateResolver
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import java.time.Instant

/**
 * Clase base abstracta para tests de integración E2E con MockMvc.
 * Configura:
 * - MySQL Testcontainers
 * - Liquibase migrations
 * - JWT mock authentication
 * - Rollback automático después de cada test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(BaseControllerIntegrationTest.TestMockConfig)
abstract class BaseControllerIntegrationTest extends Specification {

    @TestConfiguration
    static class TestMockConfig {
        private final DetachedMockFactory factory = new DetachedMockFactory()

        @Bean
        RabbitTemplate rabbitTemplate() {
            return factory.Mock(RabbitTemplate)
        }

        @Bean
        ExchangeRateResolver exchangeRateResolver() {
            return new ExchangeRateResolver(null) {
                @Override
                BigDecimal resolveRate(String symbol, java.time.LocalDate date) {
                    return BigDecimal.ONE
                }
            }
        }
    }

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
                        "org.springframework.boot.autoconfigure.amqp.RabbitStreamTemplateAutoConfiguration")
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "https://test-issuer.local/realms/test")
        registry.add("keycloak.auth-server-url", () -> "https://test-issuer.local")
    }

    @Autowired
    protected MockMvc mockMvc

    @Autowired
    protected UserRepository userRepository

    @Autowired
    protected WorkspaceRepository workspaceRepository

    @Autowired
    protected MembershipRepository membershipRepository

    @Autowired
    protected CategoryRepository categoryRepository

    @Autowired
    protected CurrencyRepository currencyRepository

    @Autowired
    protected UserSettingRepository userSettingRepository

    // ObjectMapper for JSON serialization in tests
    @Shared
    protected ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())

    // Test fixtures
    protected User testUser
    protected Workspace testWorkspace
    protected Currency testCurrency

    def setup() {
        // Create test user
        testUser = userRepository.save(User.builder()
                .email("integration-test@test.com")
                .isFirstLogin(false)
                .build())

        // Create test workspace
        testWorkspace = workspaceRepository.save(Workspace.builder()
                .name("Integration Test Workspace")
                .owner(testUser)
                .build())

        // Create membership
        membershipRepository.save(WorkspaceMember.builder()
                .user(testUser)
                .workspace(testWorkspace)
                .role(WorkspaceRole.OWNER)
                .build())

        // Set default workspace for user
        userSettingRepository.save(UserSetting.builder()
                .user(testUser)
                .settingKey(UserSettingKey.DEFAULT_WORKSPACE)
                .settingValue(testWorkspace.id)
                .build())

        // Get or create test currency
        testCurrency = currencyRepository.findBySymbol("ARS")
                .orElseGet { currencyRepository.save(Currency.builder()
                        .description("Peso Argentino")
                        .symbol("ARS")
                        .enabled(true)
                        .build())
                }
    }

    /**
     * Crea un RequestPostProcessor con JWT mock para el usuario de test.
     * Simula la autenticación de Keycloak con claims típicos.
     */
    protected RequestPostProcessor jwtAuth() {
        return jwtAuth(testUser.email, "550e8400-e29b-41d4-a716-446655440000", ["ROLE_ADMIN"])
    }

    /**
     * Crea un RequestPostProcessor con JWT mock personalizado.
     *
     * @param email el email del usuario (preferred_username claim)
     * @param subject el subject del JWT (Keycloak user id)
     * @param roles los roles del usuario
     */
    protected RequestPostProcessor jwtAuth(String email, String subject, List<String> roles) {
        def jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject(subject)
                .issuer("https://test-issuer.local/realms/test")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("preferred_username", email)
                .claim("email", email)
                .claim("realm_access", [roles: roles])
                .build()

        def authorities = roles.collect { new SimpleGrantedAuthority(it) }
        def authentication = new JwtAuthenticationToken(jwt, authorities, email)

        return SecurityMockMvcRequestPostProcessors.authentication(authentication)
    }

    /**
     * Obtiene o crea una categoría de test.
     */
    protected Category getOrCreateCategory(String description) {
        return categoryRepository.findByDescription(description)
                .orElseGet { categoryRepository.save(Category.builder()
                        .description(description)
                        .deletable(true)
                        .build())
                }
    }

    /**
     * Obtiene o crea una moneda de test.
     */
    protected Currency getOrCreateCurrency(String symbol, String description) {
        return currencyRepository.findBySymbol(symbol)
                .orElseGet { currencyRepository.save(Currency.builder()
                        .symbol(symbol)
                        .description(description)
                        .enabled(true)
                        .build())
                }
    }
}
