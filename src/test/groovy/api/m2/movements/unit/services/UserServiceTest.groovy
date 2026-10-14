package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityGateway
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.exceptions.ServiceException

import api.m2.movements.services.user.UserService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.web.client.ResourceAccessException
import spock.lang.Specification

class UserServiceTest extends Specification {

    IdentityGateway identityGateway = Mock(IdentityGateway)
    CacheManager cacheManager = new ConcurrentMapCacheManager("names")

    UserService service

    def setup() {
        service = new UserService(identityGateway, cacheManager)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "getAuthenticatedEmail - should return email from security context"() {
        given:
        def email = "test@test.com"
        setupSecurityContext(email)

        when:
        def result = service.getAuthenticatedEmail()

        then:
        result == email
    }

    def "getAuthenticatedEmail - should throw PermissionDeniedException when not authenticated"() {
        given:
        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> null
        SecurityContextHolder.setContext(securityContext)

        when:
        service.getAuthenticatedEmail()

        then:
        thrown(PermissionDeniedException)
    }

    def "getCurrentKeycloakId - should return JWT subject"() {
        given:
        def keycloakSubject = "550e8400-e29b-41d4-a716-446655440000"
        def jwt = Stub(Jwt) {
            getSubject() >> keycloakSubject
        }
        def jwtAuth = new JwtAuthenticationToken(jwt)

        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> jwtAuth
        SecurityContextHolder.setContext(securityContext)

        when:
        def result = service.getCurrentKeycloakId()

        then:
        result == keycloakSubject
    }

    def "getCurrentKeycloakId - should throw when no JWT in context"() {
        given:
        def auth = Mock(Authentication)
        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> auth
        SecurityContextHolder.setContext(securityContext)

        when:
        service.getCurrentKeycloakId()

        then:
        thrown(ServiceException)
    }

    def "getUserNamesByIds - should return a map of user id to given name"() {
        given:
        def ids = [1L, 2L]
        identityGateway.getUsersByIds(ids) >> [
                new UserMe(1L, "a@a.com", "Matias", "Fernandez", "ADMIN", null),
                new UserMe(2L, "b@b.com", "Ana", "Perez", "ADMIN", null)
        ]

        when:
        def result = service.getUserNamesByIds(ids)

        then:
        result == [1L: "Matias", 2L: "Ana"]
    }

    def "getUserNamesByIds - should only ask api-identity for the names that are not cached"() {
        given:
        identityGateway.getUsersByIds([1L]) >> [new UserMe(1L, "a@a.com", "Matias", "Fernandez", "ADMIN", null)]
        service.getUserNamesByIds([1L])

        when:
        def result = service.getUserNamesByIds([1L, 2L])

        then:
        1 * identityGateway.getUsersByIds([2L]) >> [new UserMe(2L, "b@b.com", "Ana", "Perez", "ADMIN", null)]
        result == [1L: "Matias", 2L: "Ana"]
    }

    def "getUserNamesByIds - should not call api-identity at all when every name is cached"() {
        given:
        identityGateway.getUsersByIds([1L]) >> [new UserMe(1L, "a@a.com", "Matias", "Fernandez", "ADMIN", null)]
        service.getUserNamesByIds([1L])

        when:
        def result = service.getUserNamesByIds([1L])

        then:
        0 * identityGateway.getUsersByIds(_)
        result == [1L: "Matias"]
    }

    def "getUserNamesByIds - should return an empty map instead of failing when api-identity is unreachable"() {
        given:
        identityGateway.getUsersByIds(_) >> { throw new ResourceAccessException("read timed out") }

        expect:
        service.getUserNamesByIds([1L]) == [:]
    }

    def "getUserNamesByIds - should return an empty map instead of failing on a server error from api-identity"() {
        given:
        identityGateway.getUsersByIds(_) >> { throw new ServiceException("api-identity caido") }

        expect:
        service.getUserNamesByIds([1L]) == [:]
    }

    def "getUserNamesByIds - should retry on the next call after a failure, because failures are not cached"() {
        given:
        def calls = 0
        identityGateway.getUsersByIds([1L]) >> {
            if (calls++ == 0) {
                throw new ResourceAccessException("read timed out")
            }
            return [new UserMe(1L, "a@a.com", "Matias", "Fernandez", "ADMIN", null)]
        }

        when:
        def first = service.getUserNamesByIds([1L])
        def second = service.getUserNamesByIds([1L])

        then:
        first == [:]
        second == [1L: "Matias"]
    }

    def "getUserNamesByIds - should skip users without a given name instead of failing"() {
        given:
        identityGateway.getUsersByIds([1L, 2L]) >> [
                new UserMe(1L, "a@a.com", null, null, "ADMIN", null),
                new UserMe(2L, "b@b.com", "Ana", "Perez", "ADMIN", null)
        ]

        expect:
        service.getUserNamesByIds([1L, 2L]) == [2L: "Ana"]
    }

    private void setupSecurityContext(String email) {
        def auth = Mock(Authentication)
        auth.isAuthenticated() >> true
        auth.getName() >> email

        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> auth
        SecurityContextHolder.setContext(securityContext)
    }
}
