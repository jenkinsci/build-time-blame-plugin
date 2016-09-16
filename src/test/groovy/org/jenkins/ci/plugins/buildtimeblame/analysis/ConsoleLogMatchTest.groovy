//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.AutoClone
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import hudson.plugins.timestamper.Timestamp
import spock.lang.Specification

class ConsoleLogMatchTest extends Specification {
    def 'should include helpful annotations'() {
        expect:
        ConsoleLogMatch.getAnnotation(EqualsAndHashCode) != null
        ConsoleLogMatch.getAnnotation(ToString) != null
        ConsoleLogMatch.getAnnotation(AutoClone) != null
    }

    def 'should return formatted elapsed time'() {
        given:
        def logResult = new ConsoleLogMatch(timestamp: new Timestamp(500010, 0))

        expect:
        logResult.getElapsedTime() == '08:20.010'
    }

    def 'should return formatted time taken'() {
        given:
        def logResult = new ConsoleLogMatch(timestamp: new Timestamp(500015, 0), previousElapsedTime: 50)

        expect:
        logResult.getTimeTaken() == '08:19.965'
    }

    def 'should return un-formatted time taken'() {
        given:
        def logResult = new ConsoleLogMatch(timestamp: new Timestamp(465, 0), previousElapsedTime: 15)

        expect:
        logResult.getUnFormattedTimeTaken() == 450
    }

    def 'should truncate matched line'() {
        given:
        def message = 'Something the maximum length!!'

        when:
        def logResult = new ConsoleLogMatch(matchedLine: message + ' this should be thrown away')

        then:
        logResult.matchedLine == message + '...'
    }

    def 'should not add ... if matched line is short enough'() {
        given:
        def message = 'Something the maximum length!!!!!'

        when:
        def logResult = new ConsoleLogMatch(matchedLine: message)

        then:
        logResult.matchedLine == message
    }
}
