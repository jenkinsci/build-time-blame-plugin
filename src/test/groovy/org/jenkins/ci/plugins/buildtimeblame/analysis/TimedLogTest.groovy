package org.jenkins.ci.plugins.buildtimeblame.analysis

import spock.lang.Specification

class TimedLogTest extends Specification {
    def 'should extract the trimmed text and timestamp when available'() {
        given:
        def log = 'Step 1 is done'
        def elapsedMillis = 83993
        def text = "$elapsedMillis         $log          "

        when:
        def result = TimedLog.fromText(text)

        then:
        result.log == log
        result.elapsedMillis.get() == elapsedMillis
    }

    def 'should extract the trimmed text when a timestamp is not available'() {
        given:
        def log = 'Step 2 is done'
        def text = "      $log    "

        when:
        def result = TimedLog.fromText(text)

        then:
        result.log == log
        !result.elapsedMillis.isPresent()
    }

    def 'should use the full log statement if there are no extra spaces'() {
        given:
        def log = 'Step 3 is done'
        def text = "$log"

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
        def log = 'some text for what else happened'

        when:
        def text = new TimedLog(log: log, elapsedMillis: Optional.empty() as Optional<Long>).toText()
        def result = TimedLog.fromText(text)

        then:
        result.log == log
        !result.elapsedMillis.isPresent()
    }
}
