package api.m2.movements.unit.services

import api.m2.movements.entities.User
import api.m2.movements.enums.UserSettingKey
import api.m2.movements.enums.UserType
import api.m2.movements.exceptions.EntityNotFoundException
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.UserMapper
import api.m2.movements.repositories.UserRepository
import api.m2.movements.repositories.UserSettingRepository
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import spock.lang.Specification

class UserServiceTest extends Specification {

    UserRepository userRepository = Mock(UserRepository)
    UserMapper userMapper = Mappers.getMapper(UserMapper)
    UserSettingRepository userSettingRepository = Mock(UserSettingRepository)

    UserService service

    def setup() {
        service = new UserService(
                userRepository,
                userMapper,
                userSettingRepository
        )
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "getAuthenticatedUser - should return user from security context"() {
        given:
        def email = "test@test.com"
        def user = new User(id: 1L, email: email, isFirstLogin: false)

        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.of(user)

        when:
        def result = service.getAuthenticatedUser()

        then:
        result.id == 1L
        result.email == email
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

    def "getAuthenticatedUser - should throw EntityNotFoundException when user not found"() {
        given:
        def email = "unknown@test.com"
        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.empty()

        when:
        service.getAuthenticatedUser()

        then:
        thrown(EntityNotFoundException)
    }

    def "getAuthenticatedUserRecord - should return mapped record"() {
        given:
        def email = "test@test.com"
        def user = new User(id: 1L, email: email, isFirstLogin: false)

        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.of(user)

        when:
        def result = service.getAuthenticatedUserRecord()

        then:
        result.id() == 1L
        result.email() == email
    }

    def "getUserByEmail - should return users by email list"() {
        given:
        def emails = ["user1@test.com", "user2@test.com"]
        def user1 = new User(id: 1L, email: "user1@test.com")
        def user2 = new User(id: 2L, email: "user2@test.com")

        userRepository.findByEmail(emails) >> [user1, user2]

        when:
        def result = service.getUserByEmail(emails)

        then:
        result.size() == 2
    }

    def "findUserByEmail - should return optional user"() {
        given:
        def email = "test@test.com"
        def user = new User(id: 1L, email: email)

        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.of(user)

        when:
        def result = service.findUserByEmail()

        then:
        result.isPresent()
        result.get().email == email
    }

    def "findUserByEmail - should return empty optional when user not found"() {
        given:
        def email = "unknown@test.com"
        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.empty()

        when:
        def result = service.findUserByEmail()

        then:
        result.isEmpty()
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
        thrown(IllegalStateException)
    }

    def "getMe - should return UserMeRecord for existing user"() {
        given:
        def email = "test@test.com"
        def user = new User(id: 1L, email: email, isFirstLogin: false, userType: UserType.CONSUMER, hasSeenTour: true)

        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.of(user)

        when:
        def result = service.getMe()

        then:
        result.id() == 1L
        result.email() == email
        result.isFirstLogin() == false
        result.userType() == "CONSUMER"
        result.hasSeenTour() == true
    }

    def "getMe - should return empty record for new user"() {
        given:
        def email = "newuser@test.com"
        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.empty()

        when:
        def result = service.getMe()

        then:
        result.id() == null
        result.email() == null
        result.isFirstLogin() == true
        result.userType() == null
        result.hasSeenTour() == false
    }

    def "getUsersWithMonthlySnapshotEnabled - should delegate to repository"() {
        given:
        def user1 = new User(id: 1L, email: "user1@test.com")
        def user2 = new User(id: 2L, email: "user2@test.com")

        userSettingRepository.findUsersWithSettingEnabled(UserSettingKey.MONTHLY_SUMMARY_ENABLED) >> [user1, user2]

        when:
        def result = service.getUsersWithMonthlySnapshotEnabled()

        then:
        result.size() == 2
    }

    def "getUsersWithAutoIncomeEnabled - should delegate to repository"() {
        given:
        def user = new User(id: 1L, email: "user@test.com")

        userSettingRepository.findUsersWithSettingEnabled(UserSettingKey.AUTO_INCOME_ENABLED) >> [user]

        when:
        def result = service.getUsersWithAutoIncomeEnabled()

        then:
        result.size() == 1
    }

    def "markTourAsSeen - should set hasSeenTour to true and save user"() {
        given:
        def email = "test@test.com"
        def user = new User(id: 1L, email: email, hasSeenTour: false)

        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.of(user)

        when:
        service.markTourAsSeen()

        then:
        1 * userRepository.save({ User u ->
            u.id == 1L && u.hasSeenTour == true
        })
    }

    def "markTourAsSeen - should throw EntityNotFoundException when user not found"() {
        given:
        def email = "unknown@test.com"
        setupSecurityContext(email)
        userRepository.findByEmail(email) >> Optional.empty()

        when:
        service.markTourAsSeen()

        then:
        thrown(EntityNotFoundException)
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
