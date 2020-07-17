package org.jenkins.ci.plugins.buildtimeblame.analysis

import spock.lang.Specification

class TimedLogTest extends Specification {
    def 'should convert from the expected log format from the timestamper plugin when there is a timestamp'() {
        given:
        def log = 'some text for what happened'
        def elapsedMillis = 83993
        def text = "$elapsedMillis $log"

        when:
        def result = TimedLog.fromText(text)

        then:
        result.log == log
        result.elapsedMillis.get() == elapsedMillis
    }

    def 'should convert from the expected log format from the timestamper plugin when there is no timestamp'() {
        given:
        def log = 'some text for what happened'
        def text = "  $log"

        when:
        def result = TimedLog.fromText(text)

        then:
        result.log == log
        !result.elapsedMillis.isPresent()
    }

    def 'should convert to a log line and back again when there is a timestamp'() {
        given:
        def log = 'some text for what happened'
        def elapsedMillis = 83993

        when:
        def text = new TimedLog(log: log, elapsedMillis: Optional.of(elapsedMillis)).toText()
        def result = TimedLog.fromText(text)

        then:
        result.log == log
        result.elapsedMillis.get() == elapsedMillis
    }

    def 'should convert to a log line and back again when there is no timestamp'() {
        given:
        def log = 'some text for what happened'

        when:
        def text = new TimedLog(log: log, elapsedMillis: Optional.empty() as Optional<Long>).toText()
        def result = TimedLog.fromText(text)

        then:
        result.log == log
        !result.elapsedMillis.isPresent()
    }
}
