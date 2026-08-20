package api.m2.movements.unit.services


import api.m2.movements.clients.identity.IdentityClient
import api.m2.movements.enums.UserType
import api.m2.movements.exceptions.PermissionDeniedException

import api.m2.movements.services.user.UserAddService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import spock.lang.Specification

class UserAddServiceTest extends Specification {

    IdentityClient identityClient = Mock(IdentityClient)

    UserAddService service

    def setup() {
        service = new UserAddService(identityClient)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "buildUserToAdd - should build the payload with email, givenName and familyName from JWT"() {
        given:
        def email = "newuser@test.com"
        def givenName = "John"
        def familyName = "Doe"
        setupJwtSecurityContext(email, givenName, familyName)

        when:
        def result = service.buildUserToAdd("PERSONAL")

        then:
        result.email() == email
        result.givenName() == givenName
        result.familyName() == familyName
        result.isFirstLogin() == true
        result.userType() == UserType.PERSONAL
        0 * identityClient._
    }

    def "buildUserToAdd - should build the payload with null givenName and familyName when not in JWT"() {
        given:
        def email = "newuser@test.com"
        setupJwtSecurityContext(email, null, null)

        when:
        def result = service.buildUserToAdd("ENTERPRISE")

        then:
        result.email() == email
        result.givenName() == null
        result.familyName() == null
        result.userType() == UserType.ENTERPRISE
    }

    def "buildUserToAdd - should throw PermissionDeniedException when not authenticated"() {
        given:
        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> null
        SecurityContextHolder.setContext(securityContext)

        when:
        service.buildUserToAdd("PERSONAL")

        then:
        thrown(PermissionDeniedException)
    }

    def "buildUserToAdd - should throw PermissionDeniedException when authentication is not JwtAuthenticationToken"() {
        given:
        def auth = Mock(Authentication)
        auth.isAuthenticated() >> true

        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> auth
        SecurityContextHolder.setContext(securityContext)

        when:
        service.buildUserToAdd("PERSONAL")

        then:
        thrown(PermissionDeniedException)
    }

    def "changeUserFirstLoginStatus - should delegate to IdentityClient"() {
        when:
        service.changeUserFirstLoginStatus(1L)

        then:
        1 * identityClient.changeUserFirstLoginStatus(1L)
    }

    private void setupJwtSecurityContext(String email, String givenName, String familyName) {
        def jwt = Stub(Jwt) {
            getClaimAsString("email") >> email
            getClaimAsString("given_name") >> givenName
            getClaimAsString("family_name") >> familyName
        }
        def jwtAuth = new JwtAuthenticationToken(jwt)

        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> jwtAuth
        SecurityContextHolder.setContext(securityContext)
    }
}
