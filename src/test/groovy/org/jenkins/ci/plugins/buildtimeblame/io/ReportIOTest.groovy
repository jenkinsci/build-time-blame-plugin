//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import hudson.model.Run
import org.jenkins.ci.plugins.buildtimeblame.analysis.ConsoleLogMatch
import spock.lang.Specification

class ReportIOTest extends Specification {
    Run build

    void setup() {
        GroovyMock(BlameFilePaths, global: true)
        build = Mock(Run)
        _ * BlameFilePaths.getReportFile(build) >> getTestFile()
    }

    void cleanup() {
        getTestFile().delete()
    }

    def 'should serialize list to file without derived fields'() {
        given:
        def report = [
                new ConsoleLogMatch(label: 'Finished', matchedLine: 'Did it', elapsedMillisOfNextMatch: 50, elapsedMillis: 1),
        ]

        when:
        new ReportIO(build).write(report)

        then:
        getTestFile().text == '[{"label":"Finished","matchedLine":"Did it","elapsedMillis":1,"elapsedMillisOfNextMatch":50}]'
    }

    def 'should load list from file'() {
        given:
        getTestFile().write('[{"label":"Finished","matchedLine":"Did it","elapsedMillisOfNextMatch":50,"elapsedMillis":1}]')

        when:
        def report = new ReportIO(build).readFile().get()

        then:
        report == [
                new ConsoleLogMatch(label: 'Finished', matchedLine: 'Did it', elapsedMillisOfNextMatch: 50, elapsedMillis: 1),
        ]
    }

    def 'should use same format for read and write'() {
        given:
        def expected = [
                new ConsoleLogMatch(label: 'Begin', matchedLine: 'Did it', elapsedMillisOfNextMatch: 50, elapsedMillis: 1),
                new ConsoleLogMatch(label: 'Started', matchedLine: 'Did nothing', elapsedMillisOfNextMatch: 30, elapsedMillis: 3),
                new ConsoleLogMatch(label: 'Finished', matchedLine: 'Did all', elapsedMillisOfNextMatch: 21, elapsedMillis: 5),
        ]

        when:
        new ReportIO(build).write(expected.collect { it.clone() } as List<ConsoleLogMatch>)
        def report = new ReportIO(build).readFile().get()

        then:
        report == expected
    }

    def 'should allow clearing a report'() {
        given:
        getTestFile().write('anything at all')

        when:
        new ReportIO(build).clear()

        then:
        !getTestFile().exists()
    }

    def 'should return empty optional if invalid content'() {
        given:
        getTestFile().write('any text')

        expect:
        new ReportIO(build).readFile() == Optional.empty()
    }

    def 'should  return empty optional if no content'() {
        expect:
        new ReportIO(build).readFile() == Optional.empty()
    }

    private static File getTestFile() {
        new File('tempreport.txt')
    }
}
