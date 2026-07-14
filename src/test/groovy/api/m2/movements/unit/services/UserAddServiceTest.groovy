package api.m2.movements.unit.services


import api.m2.movements.clients.IdentityClient
import api.m2.movements.movements.enums.UserType
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.identity.records.users.UserToAdd

import api.m2.movements.identity.services.user.UserAddService
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

    def "createLogInUser - should create user with email, givenName and familyName from JWT"() {
        given:
        def email = "newuser@test.com"
        def givenName = "John"
        def familyName = "Doe"
        def userType = "PERSONAL"
        setupJwtSecurityContext(email, givenName, familyName)

        def savedUser = UserToAdd.builder().id(1L).email(email).givenName(givenName).familyName(familyName)
                .isFirstLogin(true).userType(UserType.PERSONAL).build()
        identityClient.createLogInUser(_ as UserToAdd) >> savedUser

        when:
        def result = service.createLogInUser(userType)

        then:
        1 * identityClient.createLogInUser({ UserToAdd u ->
            u.email() == email &&
            u.givenName() == givenName &&
            u.familyName() == familyName &&
            u.isFirstLogin() == true &&
            u.userType() == UserType.PERSONAL
        }) >> savedUser
        result.id() == 1L
        result.email() == email
        result.givenName() == givenName
        result.familyName() == familyName
    }

    def "createLogInUser - should create user with null givenName and familyName when not in JWT"() {
        given:
        def email = "newuser@test.com"
        def userType = "ENTERPRISE"
        setupJwtSecurityContext(email, null, null)

        def savedUser = UserToAdd.builder().id(1L).email(email).isFirstLogin(true).userType(UserType.ENTERPRISE).build()
        identityClient.createLogInUser(_ as UserToAdd) >> savedUser

        when:
        def result = service.createLogInUser(userType)

        then:
        1 * identityClient.createLogInUser({ UserToAdd u ->
            u.email() == email &&
            u.givenName() == null &&
            u.familyName() == null &&
            u.userType() == UserType.ENTERPRISE
        }) >> savedUser
        result.givenName() == null
        result.familyName() == null
    }

    def "createLogInUser - should throw PermissionDeniedException when not authenticated"() {
        given:
        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> null
        SecurityContextHolder.setContext(securityContext)

        when:
        service.createLogInUser("PERSONAL")

        then:
        thrown(PermissionDeniedException)
    }

    def "createLogInUser - should throw PermissionDeniedException when authentication is not JwtAuthenticationToken"() {
        given:
        def auth = Mock(Authentication)
        auth.isAuthenticated() >> true

        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> auth
        SecurityContextHolder.setContext(securityContext)

        when:
        service.createLogInUser("PERSONAL")

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
