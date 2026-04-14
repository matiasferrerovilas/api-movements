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

    def "createLogInUser - should create user with email from security context"() {
        given:
        def email = "newuser@test.com"
        setupSecurityContext(email)

        def savedUser = new User(id: 1L, email: email, isFirstLogin: true)
        userRepository.save(_ as User) >> savedUser

        when:
        def result = service.createLogInUser()

        then:
        1 * userRepository.save({ User u ->
            u.email == email && u.isFirstLogin == true
        }) >> savedUser
        result.id == 1L
        result.email == email
        result.isFirstLogin == true
    }

    def "createLogInUser - should throw PermissionDeniedException when not authenticated"() {
        given:
        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> null
        SecurityContextHolder.setContext(securityContext)

        when:
        service.createLogInUser()

        then:
        thrown(PermissionDeniedException)
    }

    def "createLogInUser - should throw PermissionDeniedException when authentication is not authenticated"() {
        given:
        def auth = Mock(Authentication)
        auth.isAuthenticated() >> false

        def securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> auth
        SecurityContextHolder.setContext(securityContext)

        when:
        service.createLogInUser()

        then:
        thrown(PermissionDeniedException)
    }

    def "changeUserFirstLoginStatus - should update user status and type"() {
        given:
        def userId = 1L
        def userType = UserType.CONSUMER
        def existingUser = new User(id: userId, email: "test@test.com", isFirstLogin: true)

        userRepository.findById(userId) >> Optional.of(existingUser)
        userRepository.save(_ as User) >> { User u -> u }

        when:
        service.changeUserFirstLoginStatus(userType, userId)

        then:
        1 * userRepository.save({ User u ->
            u.isFirstLogin == false && u.userType == UserType.CONSUMER
        })
    }

    def "changeUserFirstLoginStatus - should throw EntityNotFoundException when user not found"() {
        given:
        def userId = 999L
        userRepository.findById(userId) >> Optional.empty()

        when:
        service.changeUserFirstLoginStatus(UserType.CONSUMER, userId)

        then:
        thrown(EntityNotFoundException)
    }

    def "changeUserFirstLoginStatus - should set COMPANY user type"() {
        given:
        def userId = 1L
        def existingUser = new User(id: userId, email: "company@test.com", isFirstLogin: true)

        userRepository.findById(userId) >> Optional.of(existingUser)
        userRepository.save(_ as User) >> { User u -> u }

        when:
        service.changeUserFirstLoginStatus(UserType.COMPANY, userId)

        then:
        1 * userRepository.save({ User u ->
            u.userType == UserType.COMPANY
        })
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
