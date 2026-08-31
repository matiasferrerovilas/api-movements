package api.m2.movements.unit.security

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.security.JwtAuthenticationConverter
import api.m2.movements.security.StompAuthChannelInterceptor
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceQueryService
import org.springframework.messaging.Message
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import spock.lang.Specification

import java.time.Instant

/**
 * Covers the STOMP-level auth gate: CONNECT requires a valid Bearer JWT, and every SUBSCRIBE is
 * checked against the connected user — workspace-scoped topics need membership, email-scoped
 * topics need the destination email to match the caller's own, the workspace "default" topic
 * needs the destination's Keycloak subject to match the caller's own, and anything unrecognized
 * is rejected rather than let through.
 */
class StompAuthChannelInterceptorTest extends Specification {

    JwtDecoder jwtDecoder = Mock(JwtDecoder)
    UserService userService = Mock(UserService)
    WorkspaceQueryService workspaceQueryService = Mock(WorkspaceQueryService)

    StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(
            jwtDecoder, new JwtAuthenticationConverter(), userService, workspaceQueryService)

    private static Jwt jwtFor(String email, String subject = email) {
        Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", email)
                .claim("sub", subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build()
    }

    private static Message<byte[]> connectMessage(String authHeader, Map<String, Object> sessionAttributes) {
        def accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader)
        }
        accessor.setSessionAttributes(sessionAttributes)
        accessor.setSessionId("session-1")
        accessor.setLeaveMutable(true)
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders())
    }

    private static Message<byte[]> subscribeMessage(String destination, def user, Map<String, Object> sessionAttributes) {
        def accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.setDestination(destination)
        accessor.setUser(user)
        accessor.setSessionAttributes(sessionAttributes)
        accessor.setSessionId("session-1")
        accessor.setLeaveMutable(true)
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders())
    }

    def "connect - rejects when there is no Authorization header"() {
        given:
        def message = connectMessage(null, [:])

        when:
        interceptor.preSend(message, null)

        then:
        thrown(MessagingException)
    }

    def "connect - rejects an invalid token"() {
        given:
        jwtDecoder.decode("bad-token") >> { throw new JwtException("invalid") }
        def message = connectMessage("Bearer bad-token", [:])

        when:
        interceptor.preSend(message, null)

        then:
        thrown(MessagingException)
    }

    def "connect - sets the user and resolves userId into the session"() {
        given:
        jwtDecoder.decode("good-token") >> jwtFor("user@example.com")
        userService.getMe() >> new UserMe(1L, "user@example.com", "N", "A", "PERSONAL", null)
        def sessionAttributes = [:]
        def message = connectMessage("Bearer good-token", sessionAttributes)

        when:
        def result = interceptor.preSend(message, null)

        then:
        def accessor = StompHeaderAccessor.wrap(result)
        accessor.getUser() != null
        accessor.getUser().getName() == "user@example.com"
        sessionAttributes.get("userId") == 1L
    }

    def "subscribe - rejects when there is no authenticated user"() {
        given:
        def message = subscribeMessage("/topic/movimientos/5/new", null, [:])

        when:
        interceptor.preSend(message, null)

        then:
        thrown(MessagingException)
    }

    def "subscribe - allows a workspace-scoped topic when the caller is a member"() {
        given:
        def user = new JwtAuthenticationToken(jwtFor("user@example.com"), [new SimpleGrantedAuthority("ROLE_FAMILY")])
        def message = subscribeMessage("/topic/movimientos/5/new", user, [userId: 1L])

        when:
        def result = interceptor.preSend(message, null)

        then:
        1 * workspaceQueryService.verifyUserIsMemberOfWorkspace(5L, 1L)
        result != null
    }

    def "subscribe - rejects a workspace-scoped topic when the caller is not a member"() {
        given:
        def user = new JwtAuthenticationToken(jwtFor("user@example.com"), [new SimpleGrantedAuthority("ROLE_FAMILY")])
        workspaceQueryService.verifyUserIsMemberOfWorkspace(_, _) >> { throw new PermissionDeniedException("no") }
        def message = subscribeMessage("/topic/servicios/99/new", user, [userId: 1L])

        when:
        interceptor.preSend(message, null)

        then:
        thrown(MessagingException)
    }

    def "subscribe - rejects a workspace-scoped topic when the session has no resolved userId"() {
        given:
        def user = new JwtAuthenticationToken(jwtFor("user@example.com"), [new SimpleGrantedAuthority("ROLE_FAMILY")])
        def message = subscribeMessage("/topic/categories/5/update", user, [:])

        when:
        interceptor.preSend(message, null)

        then:
        thrown(MessagingException)
    }

    def "subscribe - allows an email-scoped topic when it matches the caller's own email"() {
        given:
        def user = new JwtAuthenticationToken(jwtFor("user@example.com"), [])
        def message = subscribeMessage("/topic/invitations/user@example.com/new", user, [:])

        when:
        def result = interceptor.preSend(message, null)

        then:
        result != null
    }

    def "subscribe - rejects an email-scoped topic belonging to someone else"() {
        given:
        def user = new JwtAuthenticationToken(jwtFor("attacker@example.com"), [])
        def message = subscribeMessage("/topic/membership/victim@example.com/remove", user, [:])

        when:
        interceptor.preSend(message, null)

        then:
        thrown(MessagingException)
    }

    def "subscribe - allows the workspace default topic when the subject matches the caller's own"() {
        given:
        def user = new JwtAuthenticationToken(jwtFor("user@example.com", "keycloak-subject-1"), [])
        def message = subscribeMessage("/topic/workspace/default/keycloak-subject-1", user, [:])

        when:
        def result = interceptor.preSend(message, null)

        then:
        result != null
    }

    def "subscribe - rejects the workspace default topic belonging to someone else's subject"() {
        given:
        def user = new JwtAuthenticationToken(jwtFor("user@example.com", "keycloak-subject-1"), [])
        def message = subscribeMessage("/topic/workspace/default/someone-elses-subject", user, [:])

        when:
        interceptor.preSend(message, null)

        then:
        thrown(MessagingException)
    }

    def "subscribe - rejects an unrecognized destination by default"() {
        given:
        def user = new JwtAuthenticationToken(jwtFor("user@example.com"), [])
        def message = subscribeMessage("/topic/something/made/up", user, [:])

        when:
        interceptor.preSend(message, null)

        then:
        thrown(MessagingException)
    }

    def "other STOMP commands pass through unchecked"() {
        given:
        def accessor = StompHeaderAccessor.create(StompCommand.SEND)
        accessor.setDestination("/app/anything")
        def message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders())

        when:
        def result = interceptor.preSend(message, null)

        then:
        result.is(message)
    }
}
