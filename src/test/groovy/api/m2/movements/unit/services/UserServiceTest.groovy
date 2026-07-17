package api.m2.movements.unit.services

import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.exceptions.ServiceException
import api.m2.movements.records.users.UserBaseRecord

import api.m2.movements.repositories.UserSettingRepository
import api.m2.movements.services.user.UserService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import spock.lang.Specification

class UserServiceTest extends Specification {

    IdentityClient identityClient = Mock(IdentityClient)
    UserSettingRepository userSettingRepository = Mock(UserSettingRepository)

    UserService service

    def setup() {
        service = new UserService(
                identityClient,
                userSettingRepository
        )
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "getAuthenticatedUser - should resolve user via IdentityClient using email from security context"() {
        given:
        def email = "test@test.com"
        def user = new UserBaseRecord("John", 1L)

        setupSecurityContext(email)
        identityClient.getUserByEmail(email) >> user

        when:
        def result = service.getAuthenticatedUser()

        then:
        result.id() == 1L
        result.givenName() == "John"
    }

    def "getAuthenticatedUser - should throw PermissionDeniedException when not authenticated"() {
        given:
        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> null
        SecurityContextHolder.setContext(securityContext)

        when:
        service.getAuthenticatedUser()

        then:
        thrown(PermissionDeniedException)
    }

    def "getAuthenticatedEmail - should return email from security context without calling IdentityClient"() {
        given:
        def email = "test@test.com"
        setupSecurityContext(email)

        when:
        def result = service.getAuthenticatedEmail()

        then:
        result == email
        0 * identityClient.getUserByEmail(_)
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

    def "getUsersWithMonthlySnapshotEnabled - should delegate to repository"() {
        given:
        userSettingRepository.findUserIdsWithSettingEnabled(UserSettingKey.MONTHLY_SUMMARY_ENABLED) >> [1L, 2L]

        when:
        def result = service.getUsersWithMonthlySnapshotEnabled()

        then:
        result.size() == 2
    }

    def "getUsersWithAutoIncomeEnabled - should delegate to repository"() {
        given:
        userSettingRepository.findUserIdsWithSettingEnabled(UserSettingKey.AUTO_INCOME_ENABLED) >> [1L]

        when:
        def result = service.getUsersWithAutoIncomeEnabled()

        then:
        result.size() == 1
    }

    def "getUserNamesByIds - should return a map of user id to given name"() {
        given:
        def ids = [1L, 2L]
        identityClient.getUsersByIds(ids) >> [
                new UserMe(1L, "a@a.com", "Matias", "Fernandez", "ADMIN", null),
                new UserMe(2L, "b@b.com", "Ana", "Perez", "ADMIN", null)
        ]

        when:
        def result = service.getUserNamesByIds(ids)

        then:
        result == [1L: "Matias", 2L: "Ana"]
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
