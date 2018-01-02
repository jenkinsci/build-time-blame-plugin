//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import com.google.common.base.Optional
import hudson.model.Run
import hudson.plugins.timestamper.Timestamp
import hudson.plugins.timestamper.io.TimestampsReader
import org.jenkins.ci.plugins.buildtimeblame.io.CustomFileReader
import org.jenkins.ci.plugins.buildtimeblame.io.ReportIO
import spock.lang.Specification

class LogParserTest extends Specification {
    ReportIO reportIO

    void setup() {
        GroovyMock(CustomFileReader, global: true)
        reportIO = GroovyMock(ReportIO, global: true)
        _ * ReportIO.getInstance(_ as Run) >> reportIO
        _ * reportIO.readFile() >> Optional.absent()
    }

    def 'should collate console output with timestamps on matched lines'() {
        given:
        TimestampsReader timestampReader = GroovyMock(TimestampsReader, global: true)
        def build = Mock(Run)
        setupMockLog(build, 'theFirstLine', 'theSecondLine', 'theThirdLine')
        def timestamps = getRandomTimestamps(3)
        def logParser = new LogParser([
                new RelevantStep(~/.*First.*/, 'Part One', false),
                new RelevantStep(~/.*Third.*/, 'Part Two', false),
        ])
        def expected = [
                buildLogResult('Part One', 'theFirstLine', timestamps, 0, 0),
                buildLogResult('Part Two', 'theThirdLine', timestamps, 2, 0),
        ]

        when:
        BuildResult buildResult = logParser.getBuildResult(build)

        then:
        _ * new TimestampsReader(build) >> timestampReader
        _ * timestampReader.read() >>> timestamps.collect { Optional.of(it) }
        1 * reportIO.write(expected)
        buildResult.build == build
        buildResult.consoleLogMatches == expected
    }

    def 'should keep single match steps from one build to the next'() {
        given:
        TimestampsReader timestampReader = GroovyMock(TimestampsReader, global: true)
        def build1 = Mock(Run)
        setupMockLog(build1, 'theFirstLine', 'theSecondLine', 'theThirdLine')
        def build2 = Mock(Run)
        setupMockLog(build2, 'theFirstLine', 'theSecondLine', 'theThirdLine')
        def timestamps = getRandomTimestamps(3)
        def logParser = new LogParser([
                new RelevantStep(~/.*First.*/, 'Part One', true),
                new RelevantStep(~/.*Third.*/, 'Part Two', true),
        ])

        when:
        logParser.getBuildResult(build1)
        BuildResult buildResult = logParser.getBuildResult(build2)

        then:
        _ * new TimestampsReader(build2) >> timestampReader
        _ * timestampReader.read() >>> timestamps.collect { Optional.of(it) }
        buildResult.build == build2
        buildResult.consoleLogMatches.size() == 2
    }

    def 'should include the label if it is found multiple times'() {
        given:
        TimestampsReader timestampReader = GroovyMock(TimestampsReader, global: true)
        def build = Mock(Run)
        setupMockLog(build, 'aoneline', 'atwoline', 'afiveline', 'oneoneone', 'one', 'five', 'one')
        def timestamps = getRandomTimestamps(7)
        def logParser = new LogParser([
                new RelevantStep(~/.*one.*/, 'Loaded', false),
                new RelevantStep(~/.*five.*/, 'Tested', false),
        ])

        when:
        BuildResult buildResult = logParser.getBuildResult(build)

        then:
        _ * new TimestampsReader(build) >> timestampReader
        _ * timestampReader.read() >>> timestamps.collect { Optional.of(it) }
        buildResult.build == build
        buildResult.consoleLogMatches == [
                buildLogResult('Loaded', 'aoneline', timestamps, 0, 0),
                buildLogResult('Tested', 'afiveline', timestamps, 2, 0),
                buildLogResult('Loaded', 'oneoneone', timestamps, 3, 2),
                buildLogResult('Loaded', 'one', timestamps, 4, 3),
                buildLogResult('Tested', 'five', timestamps, 5, 4),
                buildLogResult('Loaded', 'one', timestamps, 6, 5),
        ]
    }

    def 'should not include the label if it is found multiple times when onlyFirstMatch enabled'() {
        given:
        def firstStep = new RelevantStep(~/.*a.*/, 'Any', false)
        def secondStep = new RelevantStep(~/.*b.*/, 'Other', true)
        def originalSteps = [firstStep, secondStep,]
        def logParser = new LogParser(originalSteps)

        when:
        logParser.relevantSteps.remove(1)

        then:
        logParser.relevantSteps == [firstStep]
        originalSteps == [firstStep, secondStep]
    }

    def 'should use copy of relevant steps rather than same reference'() {
        given:
        TimestampsReader timestampReader = GroovyMock(TimestampsReader, global: true)
        def build = Mock(Run)
        setupMockLog(build, 'aoneline', 'atwoline', 'afiveline', 'oneoneone', 'one', 'five', 'one')
        def timestamps = getRandomTimestamps(7)
        def logParser = new LogParser([
                new RelevantStep(~/.*one.*/, 'Include Every Time', false),
                new RelevantStep(~/.*five.*/, 'Include First Time', true),
        ])

        when:
        BuildResult buildResult = logParser.getBuildResult(build)

        then:
        _ * new TimestampsReader(build) >> timestampReader
        _ * timestampReader.read() >>> timestamps.collect { Optional.of(it) }
        buildResult.build == build
        buildResult.consoleLogMatches == [
                buildLogResult('Include Every Time', 'aoneline', timestamps, 0, 0),
                buildLogResult('Include First Time', 'afiveline', timestamps, 2, 0),
                buildLogResult('Include Every Time', 'oneoneone', timestamps, 3, 2),
                buildLogResult('Include Every Time', 'one', timestamps, 4, 3),
                buildLogResult('Include Every Time', 'one', timestamps, 6, 4),
        ]
    }

    def 'should throw error for enough missing timestamps'() {
        given:
        TimestampsReader timestampReader = GroovyMock(TimestampsReader, global: true)
        def build = Mock(Run)
        def timestamp1 = new Timestamp(100, 0)
        setupMockLog(build, 'line1', 'line2', 'line3', 'line4')
        def logParser = new LogParser([new RelevantStep(~/line4/, '', false)])
        logParser.maximumMissingTimestamps = 2

        when:
        logParser.getBuildResult(build)

        then:
        _ * new TimestampsReader(build) >> timestampReader
        4 * timestampReader.read() >>> [Optional.of(timestamp1), Optional.absent(), Optional.absent(), Optional.absent()]
        thrown(LogParser.TimestampMissingException)
    }

    def 'should throw error if there are no timestamps'() {
        given:
        TimestampsReader timestampReader = GroovyMock(TimestampsReader, global: true)
        def build = Mock(Run)
        setupMockLog(build, 'line1', 'line2', 'line3', 'line4')
        def logParser = new LogParser([new RelevantStep(~/line1/, '', false)])

        when:
        logParser.getBuildResult(build)

        then:
        _ * new TimestampsReader(build) >> timestampReader
        1 * timestampReader.read() >> Optional.absent()
        thrown(LogParser.TimestampMissingException)
    }

    def 'should ignore missing timestamps if no match is found after them'() {
        given:
        TimestampsReader timestampReader = GroovyMock(TimestampsReader, global: true)
        def build = Mock(Run)
        def timestamp1 = new Timestamp(100, 0)
        setupMockLog(build, 'line1', 'line2', 'line3', 'line4')
        def logParser = new LogParser([])
        logParser.maximumMissingTimestamps = 0

        when:
        logParser.getBuildResult(build)

        then:
        _ * new TimestampsReader(build) >> timestampReader
        4 * timestampReader.read() >>> [Optional.of(timestamp1), Optional.absent(), Optional.absent(), Optional.absent()]
        noExceptionThrown()
    }

    def 'should ignore the configured number of missing timestamps'() {
        given:
        TimestampsReader timestampReader = GroovyMock(TimestampsReader, global: true)
        def build = Mock(Run)
        def timestamp1 = new Timestamp(100, 0)
        setupMockLog(build, 'line1', 'line2', 'line3', 'line4')
        def logParser = new LogParser([new RelevantStep(~/line4/, '', false)])
        logParser.maximumMissingTimestamps = 3

        when:
        def result = logParser.getBuildResult(build)

        then:
        _ * new TimestampsReader(build) >> timestampReader
        4 * timestampReader.read() >>> [Optional.of(timestamp1), Optional.absent(), Optional.absent(), Optional.absent()]
        result.consoleLogMatches.size() == 1
        noExceptionThrown()
    }

    def 'should return existing results'() {
        given:
        def build = Mock(Run)
        def expected = [new ConsoleLogMatch(label: 'test')]

        when:
        BuildResult results = new LogParser([]).getBuildResult(build)

        then:
        1 * ReportIO.getInstance(build) >> reportIO
        1 * reportIO.readFile() >> Optional.of(expected)
        0 * _
        results.consoleLogMatches == expected
        results.build == build
    }

    def 'should have expected default number of ignored missing timestamps'() {
        given:
        def logParser = new LogParser([])

        expect:
        logParser.maximumMissingTimestamps == 1
    }

    private void setupMockLog(Run build, String... lines) {
        def mockLog = Mock(InputStream)
        _ * build.getLogInputStream() >> mockLog
        _ * CustomFileReader.eachLineOnlyLF(mockLog, { Closure action ->
            for (String line : lines) {
                action(line)
            }
        })
    }

    private static List<Timestamp> getRandomTimestamps(int number) {
        def result = []
        def random = new Random()
        def previousValue = random.nextInt(500000)
        for (int i = 0; i < number; i++) {
            previousValue = random.nextInt(500000) + previousValue
            result.add(new Timestamp(previousValue, 0))
        }
        return result
    }

    private
    static ConsoleLogMatch buildLogResult(String label, String line, List<Timestamp> timestamps, int index, int previousIndex) {
        return new ConsoleLogMatch(
                label: label,
                matchedLine: line,
                timestamp: timestamps[index],
                previousElapsedTime: index == 0 ? 0 : timestamps[previousIndex].elapsedMillis
        )
    }
}
