//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import com.google.common.base.Optional
import hudson.model.Run
import hudson.plugins.timestamper.io.TimestampsReader
import org.jenkins.ci.plugins.buildtimeblame.io.ReportIO

import static org.jenkins.ci.plugins.buildtimeblame.io.CustomFileReader.eachLineOnlyLF

class LogParser {
    List<RelevantStep> relevantSteps = []

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
        def timestampsReader = new TimestampsReader(run)
        def previousElapsedTime = 0

        eachLineOnlyLF(run.getLogInputStream()) { String line ->
            def nextTimestamp = timestampsReader.read()

            if (nextTimestamp.isPresent()) {
                def step = getMatchingRegex(line)

                if (step.isPresent()) {
                    def timestamp = nextTimestamp.get()

                    result.add(new ConsoleLogMatch(
                            label: step.get().label,
                            matchedLine: line,
                            timestamp: timestamp,
                            previousElapsedTime: previousElapsedTime,
                    ))
                    previousElapsedTime = timestamp.elapsedMillis
                }
            } else {
                throw new TimestampMissingException()
            }
        }
        ReportIO.getInstance(run).write(result)
        return result
    }

    Optional<RelevantStep> getMatchingRegex(String value) {
        for (RelevantStep step : relevantSteps) {
            if (step.pattern.matcher(value).matches()) {
                if (step.onlyFirstMatch) {
                    relevantSteps.remove(step)
                }
                return Optional.of(step)
            }
        }
        return Optional.absent()
    }

    public static class TimestampMissingException extends RuntimeException {
    }
}
