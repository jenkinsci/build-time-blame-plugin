//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import com.google.common.base.Optional
import hudson.model.Run
import hudson.plugins.timestamper.Timestamp
import hudson.plugins.timestamper.io.TimestampsReader
import org.jenkins.ci.plugins.buildtimeblame.io.ReportIO

import static org.jenkins.ci.plugins.buildtimeblame.io.CustomFileReader.eachLineOnlyLF

class LogParser {
    List<RelevantStep> relevantSteps = []
    int maximumMissingTimestamps = 1

    LogParser(List<RelevantStep> relevantSteps) {
        this.relevantSteps = relevantSteps.collect()
    }

    BuildResult getBuildResult(Run run) {
        def report = ReportIO.getInstance(run).readFile()

        if (report.isPresent()) {
            return new BuildResult(consoleLogMatches: report.get(), build: run)
        }

        return new BuildResult(consoleLogMatches: computeRelevantLogLines(run), build: run)
    }

    private List<ConsoleLogMatch> computeRelevantLogLines(Run run) {
        def result = []
        def previousElapsedTime = 0
        def addSingleMatchIfFound = { String label, String line, Timestamp timestamp ->
            result.add(new ConsoleLogMatch(
                    label: label,
                    matchedLine: line,
                    timestamp: timestamp,
                    previousElapsedTime: previousElapsedTime,
            ))
            previousElapsedTime = timestamp.elapsedMillis
        }

        processMatches(run, addSingleMatchIfFound)
        ReportIO.getInstance(run).write(result)
        return result
    }

    private void processMatches(Run run, Closure onMatch) {
        def numberOfMissingTimestamps = 0
        Timestamp mostRecentTimestamp

        def timestampsReader = new TimestampsReader(run)
        def steps = relevantSteps.collect()
        def inputStream = run.getLogInputStream()

        try {
            eachLineOnlyLF(inputStream).forEach { String line ->
                def nextTimestamp = timestampsReader.read()
                def step = getMatchingRegex(line, steps)

                if (nextTimestamp.isPresent()) {
                    mostRecentTimestamp = nextTimestamp.get()
                } else {
                    numberOfMissingTimestamps++
                }

                if (step.isPresent()) {
                    if (nextTimestamp.isPresent()) {
                        def timestamp = nextTimestamp.get()
                        onMatch(step.get().label, line, timestamp)
                        mostRecentTimestamp = nextTimestamp.get()
                    } else if (mostRecentTimestamp && numberOfMissingTimestamps <= maximumMissingTimestamps) {
                        onMatch(step.get().label, line, mostRecentTimestamp)
                    } else {
                        throw new TimestampMissingException()
                    }
                }
            }
        } finally {
            inputStream.close()
        }
    }

    static Optional<RelevantStep> getMatchingRegex(String value, List<RelevantStep> steps) {
        for (RelevantStep step : steps) {
            if (step.pattern.matcher(value).matches()) {
                if (step.onlyFirstMatch) {
                    steps.remove(step)
                }
                return Optional.of(step)
            }
        }
        return Optional.absent()
    }

    public static class TimestampMissingException extends RuntimeException {
    }
}
