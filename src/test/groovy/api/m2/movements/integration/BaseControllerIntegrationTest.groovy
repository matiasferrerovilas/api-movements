package api.m2.movements.integration

import api.m2.movements.entities.commons.Category
import api.m2.movements.entities.commons.Currency

import api.m2.movements.entities.integrity.UserSetting
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.repositories.CategoryRepository
import api.m2.movements.repositories.CurrencyRepository

import api.m2.movements.repositories.UserSettingRepository
import api.m2.movements.services.currencies.ExchangeRateResolver
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import groovy.json.JsonOutput
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

import static com.github.tomakehurst.wiremock.client.WireMock.*

/**
 * Clase base abstracta para tests de integración E2E con MockMvc.
 * Configura:
 * - MySQL Testcontainers
 * - Redis Testcontainers (requerido por @Cacheable de CurrencyAddService/CacheConfiguration)
 * - Liquibase migrations
 * - JWT mock authentication
 * - Rollback automático después de cada test
 *
 * User/Workspace/membresías ahora viven en api-identity: en vez de mockear la
 * interfaz IdentityClient, se levanta un WireMockServer y se apunta
 * identity.base-url ahí. Así el RestClient/HttpServiceProxyFactory real (URLs,
 * headers, (de)serialización JSON) queda ejercitado en los tests de
 * integración, no solo el contrato Java del cliente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestMockConfig)
// RabbitTemplate y ExchangeRateResolver no son llamadas HTTP (AMQP el primero,
// el segundo se simplifica para evitar acoplar el cache de tipo de cambio a
// WireMock) así que siguen mockeados como bean. Spring Boot no permite bean
// overriding por defecto: debe ser un @TestPropertySource (no
// @DynamicPropertySource) para que también aplique durante el procesamiento
// AOT de tests (processTestAot).
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
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
                BigDecimal resolveRate(String symbol, LocalDate date) {
                    return BigDecimal.ONE
                }
            }
        }
    }

    @Shared
    static WireMockServer identityMock

    static {
        identityMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        identityMock.start()
        configureFor("localhost", identityMock.port())
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
            .withTmpFs(["/var/lib/mysql": "rw"])
            .withCommand(
                    "--innodb-flush-log-at-trx-commit=0",
                    "--innodb-doublewrite=0",
                    "--skip-log-bin"
            )

    @Container
    @Shared
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true)

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
        registry.add("identity.base-url", () -> "http://localhost:${identityMock.port()}".toString())
    }

    @Autowired
    protected MockMvc mockMvc

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
    protected static final String TEST_USER_EMAIL = "integration-test@test.com"
    protected Long testUserId = 1L
    protected Long testWorkspaceId = 1L
    protected Currency testCurrency

    def setup() {
        identityMock.resetAll()

        // User/Workspace/membresía ahora se resuelven vía IdentityClient (api-identity),
        // stubeado acá con WireMock. Tests concretos pueden registrar un stubFor(...)
        // más específico en su propio "given:" para pisar estos defaults.
        stubFor(get(urlPathEqualTo("/v1/users/me"))
                .willReturn(okJson(JsonOutput.toJson([
                        id        : testUserId,
                        email     : TEST_USER_EMAIL,
                        givenName : "Integration",
                        familyName: "Test",
                        userType  : "PERSONAL",
                        metadata  : [isFirstLogin: false, hasSeenTour: true, userRole: []]
                ]))))

        stubFor(get(urlPathMatching("/v1/workspaces/\\d+/members/\\d+"))
                .willReturn(aResponse().withStatus(200)))

        stubFor(post(urlPathEqualTo("/v1/users"))
                .willReturn(okJson(JsonOutput.toJson([
                        id          : testUserId,
                        email       : TEST_USER_EMAIL,
                        givenName   : "Integration",
                        familyName  : "Test",
                        isFirstLogin: true,
                        userType    : "PERSONAL"
                ]))))

        stubFor(patch(urlPathMatching("/v1/onboarding/\\d+/first-login"))
                .willReturn(aResponse().withStatus(200)))

        stubFor(post(urlPathEqualTo("/v1/workspaces"))
                .willReturn(okJson(JsonOutput.toJson([[id: testWorkspaceId, description: "DEFAULT"]]))))

        stubFor(get(urlPathEqualTo("/v1/workspaces/members"))
                .willReturn(okJson(JsonOutput.toJson([[
                        id           : testWorkspaceId,
                        workspaceId  : testWorkspaceId,
                        workspaceName: "Familia",
                        metadata     : [members: [], role: "OWNER", joinedAt: null, isDefault: true]
                ]]))))

        stubFor(get(urlPathEqualTo("/v1/users"))
                .willReturn(okJson(JsonOutput.toJson([[
                        id        : testUserId,
                        email     : TEST_USER_EMAIL,
                        givenName : "Integration",
                        familyName: "Test",
                        userType  : "PERSONAL",
                        metadata  : [isFirstLogin: false, hasSeenTour: true, userRole: []]
                ]]))))

        stubFor(delete(urlPathMatching("/v1/workspaces/\\d+"))
                .willReturn(aResponse().withStatus(204)))

        // Set default workspace for user (sigue siendo local)
        userSettingRepository.save(UserSetting.builder()
                .userId(testUserId)
                .settingKey(UserSettingKey.DEFAULT_WORKSPACE)
                .settingValue(testWorkspaceId)
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
        return jwtAuth(TEST_USER_EMAIL, "550e8400-e29b-41d4-a716-446655440000", ["ROLE_ADMIN"])
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
