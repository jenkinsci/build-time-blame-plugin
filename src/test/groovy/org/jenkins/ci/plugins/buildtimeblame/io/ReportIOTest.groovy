//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import org.jenkins.ci.plugins.buildtimeblame.analysis.ConsoleLogMatch
import com.google.common.base.Optional
import hudson.model.Run
import hudson.plugins.timestamper.Timestamp
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
                new ConsoleLogMatch(label: 'Finished', matchedLine: 'Did it', previousElapsedTime: 50, timestamp: new Timestamp(1, 2)),
        ]

        when:
        new ReportIO(build).write(report)

        then:
        getTestFile().text == '[{"label":"Finished","matchedLine":"Did it","previousElapsedTime":50,"timestamp":{"elapsedMillis":1,"elapsedMillisKnown":true,"millisSinceEpoch":2}}]'
    }

    def 'should load list from file'() {
        given:
        getTestFile().write('[{"label":"Finished","matchedLine":"Did it","previousElapsedTime":50,"timestamp":{"elapsedMillis":1,"millisSinceEpoch":2}}]')

        when:
        def report = new ReportIO(build).readFile().get()

        then:
        report == [
                new ConsoleLogMatch(label: 'Finished', matchedLine: 'Did it', previousElapsedTime: 50, timestamp: new Timestamp(1, 2)),
        ]
    }

    def 'should use same format for read and write'() {
        given:
        def expected = [
                new ConsoleLogMatch(label: 'Begin', matchedLine: 'Did it', previousElapsedTime: 50, timestamp: new Timestamp(1, 2)),
                new ConsoleLogMatch(label: 'Started', matchedLine: 'Did nothing', previousElapsedTime: 30, timestamp: new Timestamp(3, 4)),
                new ConsoleLogMatch(label: 'Finished', matchedLine: 'Did all', previousElapsedTime: 21, timestamp: new Timestamp(5, 6)),
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
        new ReportIO(build).readFile() == Optional.absent()
    }

    def 'should  return empty optional if no content'() {
        expect:
        new ReportIO(build).readFile() == Optional.absent()
    }

    private static File getTestFile() {
        new File('tempreport.txt')
    }
}
