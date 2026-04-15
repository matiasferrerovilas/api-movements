package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.enums.UserType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.repositories.UserRepository
import api.m2.movements.services.user.UserAddService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import spock.lang.Specification

class UserAddServiceTest extends Specification {

    UserRepository userRepository = Mock(UserRepository)

    UserAddService service

    def setup() {
        service = new UserAddService(userRepository)
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

        def savedUser = new User(id: 1L, email: email, givenName: givenName, familyName: familyName, isFirstLogin: true, userType: UserType.PERSONAL)
        userRepository.save(_ as User) >> savedUser

        when:
        def result = service.createLogInUser(userType)

        then:
        1 * userRepository.save({ User u ->
            u.email == email &&
            u.givenName == givenName &&
            u.familyName == familyName &&
            u.isFirstLogin == true &&
            u.userType == UserType.PERSONAL
        }) >> savedUser
        result.id == 1L
        result.email == email
        result.givenName == givenName
        result.familyName == familyName
        result.isFirstLogin == true
        result.userType == UserType.PERSONAL
    }

    def "createLogInUser - should create user with null givenName and familyName when not in JWT"() {
        given:
        def email = "newuser@test.com"
        def userType = "ENTERPRISE"
        setupJwtSecurityContext(email, null, null)

        def savedUser = new User(id: 1L, email: email, givenName: null, familyName: null, isFirstLogin: true, userType: UserType.ENTERPRISE)
        userRepository.save(_ as User) >> savedUser

        when:
        def result = service.createLogInUser(userType)

        then:
        1 * userRepository.save({ User u ->
            u.email == email &&
            u.givenName == null &&
            u.familyName == null &&
            u.isFirstLogin == true &&
            u.userType == UserType.ENTERPRISE
        }) >> savedUser
        result.givenName == null
        result.familyName == null
        result.userType == UserType.ENTERPRISE
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

    def "changeUserFirstLoginStatus - should update user status and type"() {
        given:
        def userId = 1L
        def userType = UserType.PERSONAL
        def existingUser = new User(id: userId, email: "test@test.com", isFirstLogin: true)

        userRepository.findById(userId) >> Optional.of(existingUser)
        userRepository.save(_ as User) >> { User u -> u }

        when:
        service.changeUserFirstLoginStatus(userType, userId)

        then:
        1 * userRepository.save({ User u ->
            u.isFirstLogin == false && u.userType == UserType.PERSONAL
        })
    }

    def "changeUserFirstLoginStatus - should throw EntityNotFoundException when user not found"() {
        given:
        def userId = 999L
        userRepository.findById(userId) >> Optional.empty()

        when:
        service.changeUserFirstLoginStatus(UserType.PERSONAL, userId)

        then:
        thrown(EntityNotFoundException)
    }

    def "changeUserFirstLoginStatus - should set ENTERPRISE user type"() {
        given:
        def userId = 1L
        def existingUser = new User(id: userId, email: "company@test.com", isFirstLogin: true)

        userRepository.findById(userId) >> Optional.of(existingUser)
        userRepository.save(_ as User) >> { User u -> u }

        when:
        service.changeUserFirstLoginStatus(UserType.ENTERPRISE, userId)

        then:
        1 * userRepository.save({ User u ->
            u.userType == UserType.ENTERPRISE
        })
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
