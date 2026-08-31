package api.m2.movements.integration

import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

import java.time.Instant

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Covers CacheConfiguration.IDENTITY_CACHE: UserService.getMe()/getMe(workspaceId) and
 * WorkspaceQueryService.verifyUserIsMemberOfWorkspace are @Cacheable for 5hs — before this, every
 * request that touched a @RequiresMembership endpoint (almost all of them) made a fresh HTTP
 * round-trip to api-identity, with no cache, timeout or circuit breaker.
 */
class IdentityCacheIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    WorkspaceQueryService workspaceQueryService

    def "GET /v1/users/me - only hits api-identity once across repeated calls"() {
        when:
        mockMvc.perform(get("/v1/users/me").with(jwtAuth())).andExpect(status().isOk())
        mockMvc.perform(get("/v1/users/me").with(jwtAuth())).andExpect(status().isOk())
        mockMvc.perform(get("/v1/users/me").with(jwtAuth())).andExpect(status().isOk())

        then:
        // Ambas variantes (sin workspaceId, con workspaceId=testWorkspaceId) cachean por
        // separado, pero cada una debería resolverse UNA sola vez contra api-identity —
        // sumando las dos, nunca más de 2 llamadas reales pese a las 3 requests HTTP.
        verify(2, getRequestedFor(urlPathEqualTo("/v1/users/me")))
    }

    def "verifyUserIsMemberOfWorkspace - a granted membership is cached, so it's only verified against api-identity once"() {
        given:
        // Llamando al servicio directo (no vía MockMvc) para probar el @Cacheable en sí, sin
        // pasar por HTTP/aspecto — necesita el mismo Authentication que jwtAuth() arma para las
        // requests reales, porque IdentityClientConfig lo lee del SecurityContext para el header
        // saliente hacia api-identity.
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

        when:
        3.times { workspaceQueryService.verifyUserIsMemberOfWorkspace(testWorkspaceId, testUserId) }

        then:
        verify(1, getRequestedFor(urlPathMatching("/v1/workspaces/${testWorkspaceId}/members/${testUserId}")))

        cleanup:
        SecurityContextHolder.clearContext()
    }
}
