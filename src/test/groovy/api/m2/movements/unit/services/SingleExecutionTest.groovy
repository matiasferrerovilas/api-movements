package api.m2.movements.unit.services

import api.m2.movements.helpers.SingleExecution
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SingleExecutionTest extends Specification {

    def "runIfIdle - should run the task when nothing is running"() {
        given:
        def runs = 0

        when:
        def accepted = new SingleExecution().runIfIdle { runs++ }

        then:
        accepted
        runs == 1
    }

    def "runIfIdle - should skip a second call that arrives while the first is still running"() {
        given:
        def execution = new SingleExecution()
        def started = new CountDownLatch(1)
        def release = new CountDownLatch(1)
        def first = Thread.start {
            execution.runIfIdle {
                started.countDown()
                release.await(5, TimeUnit.SECONDS)
            }
        }
        started.await(5, TimeUnit.SECONDS)
        def secondRan = false

        when:
        def accepted = execution.runIfIdle { secondRan = true }
        release.countDown()
        first.join(5000)

        then:
        !accepted
        !secondRan
    }

    def "runIfIdle - should allow a new run after the previous one failed"() {
        given:
        def execution = new SingleExecution()

        when:
        execution.runIfIdle { throw new IllegalStateException("boom") }

        then:
        thrown(IllegalStateException)

        and:
        execution.runIfIdle { }
    }
}
