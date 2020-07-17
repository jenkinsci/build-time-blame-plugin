//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import hudson.model.Run
import hudson.plugins.timestamper.api.TimestamperAPI
import org.jenkins.ci.plugins.buildtimeblame.io.ReportIO
import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.IntStream

class LogParserTest extends Specification {
    ReportIO reportIO
    TimestamperAPI timestamperAPI

    void setup() {
        timestamperAPI = GroovyMock(TimestamperAPI, global: true)
        _ * TimestamperAPI.get() >> timestamperAPI

        reportIO = GroovyMock(ReportIO, global: true)
        _ * ReportIO.getInstance(_ as Run) >> reportIO
        _ * reportIO.readFile() >> Optional.empty()
    }

    def 'should collate console output with timestamps on matched lines'() {
        given:
        def build = Mock(Run)
        def logs = ['theFirstLine', 'theSecondLine', 'theThirdLine', 'theFourthLine']
        def timestamps = getRandomTimestamps(logs.size())
        setupMockTimestamperLog(build, withTimestamps(logs, timestamps))
        def expected = [
                buildLogResult('Part One', 'theFirstLine', timestamps, 0, 2),
                buildLogResult('Part Two', 'theThirdLine', timestamps, 2, -1),
        ]

        def logParser = new LogParser([
                new RelevantStep(~/.*First.*/, 'Part One', false),
                new RelevantStep(~/.*Third.*/, 'Part Two', false),
        ])

        when:
        BuildResult buildResult = logParser.getBuildResult(build)

        then:
        buildResult.build == build
        buildResult.consoleLogMatches == expected
//        1 * reportIO.write(expected)
    }

    def 'should keep single match steps from one build to the next'() {
        given:
        def logs = ['theFirstLine', 'theSecondLine', 'theThirdLine']
        def build1 = Mock(Run)
        setupMockTimestamperLog(build1, withTimestamps(logs, getRandomTimestamps(logs.size())))
        def build2 = Mock(Run)
        def timestamps = getRandomTimestamps(logs.size())
        setupMockTimestamperLog(build2, withTimestamps(logs, timestamps))

        def logParser = new LogParser([
                new RelevantStep(~/.*First.*/, 'Part One', true),
                new RelevantStep(~/.*Third.*/, 'Part Two', true),
        ])

        when:
        logParser.getBuildResult(build1)
        BuildResult buildResult = logParser.getBuildResult(build2)

        then:
        buildResult.build == build2
        buildResult.consoleLogMatches.size() == 2
    }

    def 'should include the label if it is found multiple times'() {
        given:
        def build = Mock(Run)
        def logs = ['aoneline', 'atwoline', 'afiveline', 'oneoneone', 'one', 'five', 'one']
        def timestamps = getRandomTimestamps(logs.size())
        setupMockTimestamperLog(build, withTimestamps(logs, timestamps))
        def logParser = new LogParser([
                new RelevantStep(~/.*one.*/, 'Loaded', false),
                new RelevantStep(~/.*five.*/, 'Tested', false),
        ])

        when:
        BuildResult buildResult = logParser.getBuildResult(build)

        then:
        buildResult.build == build
        buildResult.consoleLogMatches == [
                buildLogResult('Loaded', 'aoneline', timestamps, 0, 2),
                buildLogResult('Tested', 'afiveline', timestamps, 2, 3),
                buildLogResult('Loaded', 'oneoneone', timestamps, 3, 4),
                buildLogResult('Loaded', 'one', timestamps, 4, 5),
                buildLogResult('Tested', 'five', timestamps, 5, 6),
                buildLogResult('Loaded', 'one', timestamps, 6, -1),
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
        def build = Mock(Run)
        def logs = ['aoneline', 'atwoline', 'afiveline', 'oneoneone', 'one', 'five', 'one']
        def timestamps = getRandomTimestamps(logs.size())
        setupMockTimestamperLog(build, withTimestamps(logs, timestamps))

        def logParser = new LogParser([
                new RelevantStep(~/.*one.*/, 'Include Every Time', false),
                new RelevantStep(~/.*five.*/, 'Include First Time', true),
        ])

        when:
        BuildResult buildResult = logParser.getBuildResult(build)

        then:
        buildResult.build == build
        buildResult.consoleLogMatches == [
                buildLogResult('Include Every Time', 'aoneline', timestamps, 0, 2),
                buildLogResult('Include First Time', 'afiveline', timestamps, 2, 3),
                buildLogResult('Include Every Time', 'oneoneone', timestamps, 3, 4),
                buildLogResult('Include Every Time', 'one', timestamps, 4, 6),
                buildLogResult('Include Every Time', 'one', timestamps, 6, -1),
        ]
    }

    def 'should throw error if there are no timestamps'() {
        given:
        def build = Mock(Run)
        setupMockTimestamperLog(build, [
                new TimedLog(log: 'line1'),
                new TimedLog(log: 'line2'),
                new TimedLog(log: 'line3')
        ])

        def logParser = new LogParser([new RelevantStep(~/line1/, '', false)])

        when:
        logParser.getBuildResult(build)

        then:
        thrown(LogParser.TimestampMissingException)
    }

    def 'should use the previous timestamp for missing timestamps'() {
        given:
        def build = Mock(Run)
        def timestamps = getRandomTimestamps(2)
        setupMockTimestamperLog(build, [
                new TimedLog(log: 'line1', elapsedMillis: Optional.of(timestamps[0])),
                new TimedLog(log: 'line2'),
                new TimedLog(log: 'line3', elapsedMillis: Optional.of(timestamps[1]))
        ])

        def logParser = new LogParser([
                new RelevantStep(~/line2/, 'First', false),
                new RelevantStep(~/line3/, 'Second', false),
        ])

        when:
        BuildResult buildResult = logParser.getBuildResult(build)

        then:
        buildResult.build == build
        buildResult.consoleLogMatches == [
                buildLogResult('First', 'line2', timestamps, 0, 1),
                buildLogResult('Second', 'line3', timestamps, 1, -1),
        ]
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

    private void setupMockTimestamperLog(Run build, List<TimedLog> lines) {
        def mockReader = Mock(BufferedReader)
        1 * timestamperAPI.read(build, 'appendLog&elapsed=S') >> mockReader
        1 * mockReader.lines() >> lines.stream().map({ line -> line.toText() })
        1 * mockReader.close()
    }

    private static List<Long> getRandomTimestamps(int number) {
        def result = []
        def random = new Random()
        def previousValue = nextPositiveInt(random)

        for (int i = 0; i < number; i++) {
            previousValue = nextPositiveInt(random) + previousValue
            result.add(previousValue)
        }
        return result
    }

    private static int nextPositiveInt(Random random) {
        random.nextInt(500000) + 1
    }

    private static List<TimedLog> withTimestamps(List<String> lines, List<Long> timestamps) {
        return IntStream.range(0, lines.size())
                .mapToObj({ Integer index ->
                    return new TimedLog(log: lines[index], elapsedMillis: Optional.of(timestamps[index]))
                })
                .collect(Collectors.toList())
    }

    private static ConsoleLogMatch buildLogResult(String label, String line, List<Long> elapsedMillis, int index, int nextIndex) {
        return new ConsoleLogMatch(
                label: label,
                matchedLine: line,
                elapsedMillis: elapsedMillis[index],
                elapsedMillisOfNextMatch: elapsedMillis[nextIndex]
        )
    }
}
