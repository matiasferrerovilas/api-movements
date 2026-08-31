package api.m2.movements.unit.configuration

import api.m2.movements.configuration.RateLimitInterceptor
import api.m2.movements.exceptions.RateLimitExceededException
import api.m2.movements.services.ratelimit.RateLimiterService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

import java.time.Duration

class RateLimitInterceptorTest extends Specification {

    RateLimiterService rateLimiterService = Mock(RateLimiterService)
    HttpServletRequest request = Mock(HttpServletRequest)
    HttpServletResponse response = Mock(HttpServletResponse)

    RateLimitInterceptor interceptor = new RateLimitInterceptor(rateLimiterService)

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "preHandle - lets the request through when the limiter allows it"() {
        given:
        authenticateAs("user@example.com")
        rateLimiterService.tryAcquire("rate-limit:http:user@example.com", 200, Duration.ofMinutes(1)) >> true

        expect:
        interceptor.preHandle(request, response, new Object())
    }

    def "preHandle - throws RateLimitExceededException when the limiter rejects"() {
        given:
        authenticateAs("user@example.com")
        rateLimiterService.tryAcquire(_, _, _) >> false

        when:
        interceptor.preHandle(request, response, new Object())

        then:
        thrown(RateLimitExceededException)
    }

    def "preHandle - lets the request through when there is no authentication"() {
        given:
        def securityContext = Stub(SecurityContext) { getAuthentication() >> null }
        SecurityContextHolder.setContext(securityContext)

        when:
        def result = interceptor.preHandle(request, response, new Object())

        then:
        result
        0 * rateLimiterService.tryAcquire(_, _, _)
    }

    private void authenticateAs(String email) {
        def authentication = Stub(Authentication) {
            getName() >> email
            isAuthenticated() >> true
        }
        def securityContext = Stub(SecurityContext) { getAuthentication() >> authentication }
        SecurityContextHolder.setContext(securityContext)
    }
}
