package api.m2.movements.unit.services

import api.m2.movements.clients.identity.response.UserMe
import api.m2.movements.entities.UserStreak
import api.m2.movements.repositories.UserStreakRepository
import api.m2.movements.services.gamification.StreakService
import api.m2.movements.services.user.UserService
import api.m2.movements.services.workspaces.WorkspaceContextService
import spock.lang.Specification

import java.time.LocalDate

class StreakServiceTest extends Specification {

    UserStreakRepository userStreakRepository = Mock()
    UserService userService = Mock()
    WorkspaceContextService workspaceContextService = Mock()

    StreakService service

    def setup() {
        service = new StreakService(userStreakRepository, userService, workspaceContextService)
    }

    // --- recordActivity ---

    def "recordActivity - starts a new streak at 1 when the user has never registered anything"() {
        given:
        userStreakRepository.findByUserIdAndWorkspaceId(1L, 10L) >> Optional.empty()

        when:
        service.recordActivity(1L, 10L, LocalDate.of(2026, 8, 20))

        then:
        1 * userStreakRepository.save({ UserStreak s ->
            s.userId == 1L && s.workspaceId == 10L &&
            s.currentStreak == 1 && s.longestStreak == 1 &&
            s.lastActivityDate == LocalDate.of(2026, 8, 20)
        })
    }

    def "recordActivity - increments the streak when the activity is exactly one day after the last one"() {
        given:
        def existing = UserStreak.builder().userId(1L).workspaceId(10L)
                .currentStreak(4).longestStreak(4).lastActivityDate(LocalDate.of(2026, 8, 19)).build()
        userStreakRepository.findByUserIdAndWorkspaceId(1L, 10L) >> Optional.of(existing)

        when:
        service.recordActivity(1L, 10L, LocalDate.of(2026, 8, 20))

        then:
        1 * userStreakRepository.save({ UserStreak s -> s.currentStreak == 5 && s.longestStreak == 5 })
    }

    def "recordActivity - resets to 1 when there is a gap of more than one day"() {
        given:
        def existing = UserStreak.builder().userId(1L).workspaceId(10L)
                .currentStreak(7).longestStreak(7).lastActivityDate(LocalDate.of(2026, 8, 15)).build()
        userStreakRepository.findByUserIdAndWorkspaceId(1L, 10L) >> Optional.of(existing)

        when:
        service.recordActivity(1L, 10L, LocalDate.of(2026, 8, 20))

        then:
        1 * userStreakRepository.save({ UserStreak s -> s.currentStreak == 1 && s.longestStreak == 7 })
    }

    def "recordActivity - is a no-op when activity for that day was already recorded"() {
        given:
        def existing = UserStreak.builder().userId(1L).workspaceId(10L)
                .currentStreak(3).longestStreak(3).lastActivityDate(LocalDate.of(2026, 8, 20)).build()
        userStreakRepository.findByUserIdAndWorkspaceId(1L, 10L) >> Optional.of(existing)

        when:
        service.recordActivity(1L, 10L, LocalDate.of(2026, 8, 20))

        then:
        0 * userStreakRepository.save(_)
    }

    def "recordActivity - keeps the historical longest streak even after a reset"() {
        given:
        def existing = UserStreak.builder().userId(1L).workspaceId(10L)
                .currentStreak(2).longestStreak(10).lastActivityDate(LocalDate.of(2026, 8, 1)).build()
        userStreakRepository.findByUserIdAndWorkspaceId(1L, 10L) >> Optional.of(existing)

        when:
        service.recordActivity(1L, 10L, LocalDate.of(2026, 8, 20))

        then:
        1 * userStreakRepository.save({ UserStreak s -> s.currentStreak == 1 && s.longestStreak == 10 })
    }

    // --- getStreak ---

    def "getStreak - returns zeros when the user has no streak record yet"() {
        given:
        userService.getMe() >> new UserMe(1L, "a@b.com", "A", "B", "PERSONAL", null)
        workspaceContextService.getActiveWorkspaceId() >> 10L
        userStreakRepository.findByUserIdAndWorkspaceId(1L, 10L) >> Optional.empty()

        when:
        def result = service.getStreak()

        then:
        result.currentStreak() == 0
        result.longestStreak() == 0
        result.lastActivityDate() == null
    }

    def "getStreak - reports the stored streak when the last activity was today"() {
        given:
        userService.getMe() >> new UserMe(1L, "a@b.com", "A", "B", "PERSONAL", null)
        workspaceContextService.getActiveWorkspaceId() >> 10L
        def streak = UserStreak.builder().userId(1L).workspaceId(10L)
                .currentStreak(5).longestStreak(8).lastActivityDate(LocalDate.now()).build()
        userStreakRepository.findByUserIdAndWorkspaceId(1L, 10L) >> Optional.of(streak)

        when:
        def result = service.getStreak()

        then:
        result.currentStreak() == 5
        result.longestStreak() == 8
    }

    def "getStreak - reports a broken (zero) current streak when the last activity was more than a day ago, but keeps the record"() {
        given:
        userService.getMe() >> new UserMe(1L, "a@b.com", "A", "B", "PERSONAL", null)
        workspaceContextService.getActiveWorkspaceId() >> 10L
        def streak = UserStreak.builder().userId(1L).workspaceId(10L)
                .currentStreak(5).longestStreak(8).lastActivityDate(LocalDate.now().minusDays(3)).build()
        userStreakRepository.findByUserIdAndWorkspaceId(1L, 10L) >> Optional.of(streak)

        when:
        def result = service.getStreak()

        then:
        result.currentStreak() == 0
        result.longestStreak() == 8
    }
}
