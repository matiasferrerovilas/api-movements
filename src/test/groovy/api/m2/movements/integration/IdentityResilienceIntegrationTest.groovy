package api.m2.movements.integration

import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.verify

/**
 * Los nombres de workspace y de usuario que vienen de api-identity son solo para mostrar: una
 * api-identity lenta o caída no tiene que colgar ni tumbar la operación que los pidió (por
 * ejemplo, perder el alta de un movimiento por no poder ponerle el nombre al evento), y lo ya
 * resuelto tiene que servirse del cache (CacheConfiguration.NAMES_CACHE).
 *
 * El read-timeout de los tests es 1s (ver src/test/resources/application.yaml): un delay de 4s
 * tiene que cortarse mucho antes de cumplirse.
 */
class IdentityResilienceIntegrationTest extends BaseControllerIntegrationTest {

    private static final int SLOW_RESPONSE_MS = 4000
    private static final long MAX_WAIT_MS = 3000

    @Autowired
    WorkspaceQueryService workspaceQueryService

    @Autowired
    UserService userService

    def setup() {
        def jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("550e8400-e29b-41d4-a716-446655440000")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("email", TEST_USER_EMAIL)
                .claim("realm_access", [roles: ["ROLE_ADMIN"]])
                .build()
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, [new SimpleGrantedAuthority("ROLE_ADMIN")], TEST_USER_EMAIL))
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    // Ids altos y al azar para no chocar con claves de NAMES_CACHE que quedaron en el Redis
    // reutilizado de otras corridas.
    private static Long uniqueId() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L)
    }

    private static void stubWorkspace(Long workspaceId, String name) {
        stubFor(get(urlPathEqualTo("/v1/workspaces/members"))
                .willReturn(okJson(JsonOutput.toJson([[
                        id           : workspaceId,
                        workspaceId  : workspaceId,
                        workspaceName: name,
                        metadata     : [members: [], role: "OWNER", joinedAt: null, isDefault: true]
                ]]))))
    }

    private static void stubUser(Long userId, String givenName) {
        stubFor(get(urlPathEqualTo("/v1/users"))
                .willReturn(okJson(JsonOutput.toJson([[
                        id        : userId,
                        email     : "user-${userId}@test.com".toString(),
                        givenName : givenName,
                        familyName: "Test",
                        userType  : "PERSONAL",
                        metadata  : [isFirstLogin: false, hasSeenTour: true, userRole: []]
                ]]))))
    }

    def "findWorkspaceNameById - should return null without waiting for a slow api-identity"() {
        given:
        stubFor(get(urlPathEqualTo("/v1/workspaces/members"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(SLOW_RESPONSE_MS)))

        when:
        def started = System.nanoTime()
        def name = workspaceQueryService.findWorkspaceNameById(uniqueId())
        def elapsedMs = (System.nanoTime() - started) / 1_000_000

        then:
        name == null
        elapsedMs < MAX_WAIT_MS
    }

    def "findWorkspaceNameById - should return null on a server error from api-identity"() {
        given:
        stubFor(get(urlPathEqualTo("/v1/workspaces/members")).willReturn(aResponse().withStatus(500)))

        expect:
        workspaceQueryService.findWorkspaceNameById(uniqueId()) == null
    }

    def "findWorkspaceNameById - should hit api-identity once and serve the rest from the cache"() {
        given:
        def workspaceId = uniqueId()
        stubWorkspace(workspaceId, "Casa")

        when:
        def names = (1..3).collect { workspaceQueryService.findWorkspaceNameById(workspaceId) }

        then:
        names == ["Casa", "Casa", "Casa"]
        verify(1, getRequestedFor(urlPathEqualTo("/v1/workspaces/members")))
    }

    def "findWorkspaceNameById - should not cache a failure, so the next call recovers the name"() {
        given:
        def workspaceId = uniqueId()
        stubFor(get(urlPathEqualTo("/v1/workspaces/members")).willReturn(aResponse().withStatus(500)))

        when:
        def whileDown = workspaceQueryService.findWorkspaceNameById(workspaceId)
        stubWorkspace(workspaceId, "Casa")
        def afterRecovery = workspaceQueryService.findWorkspaceNameById(workspaceId)

        then:
        whileDown == null
        afterRecovery == "Casa"
    }

    def "getUserNamesByIds - should return an empty map without waiting for a slow api-identity"() {
        given:
        stubFor(get(urlPathEqualTo("/v1/users"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(SLOW_RESPONSE_MS)))

        when:
        def started = System.nanoTime()
        def names = userService.getUserNamesByIds([uniqueId()])
        def elapsedMs = (System.nanoTime() - started) / 1_000_000

        then:
        names == [:]
        elapsedMs < MAX_WAIT_MS
    }

    def "getUserNamesByIds - should hit api-identity once and serve the rest from the cache"() {
        given:
        def userId = uniqueId()
        stubUser(userId, "Lucia")

        when:
        def results = (1..3).collect { userService.getUserNamesByIds([userId]) }

        then:
        results.every { it == [(userId): "Lucia"] }
        verify(1, getRequestedFor(urlPathEqualTo("/v1/users")))
    }
}
