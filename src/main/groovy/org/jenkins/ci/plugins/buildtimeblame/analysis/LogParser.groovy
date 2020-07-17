//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import hudson.model.Run
import hudson.plugins.timestamper.api.TimestamperAPI
import org.jenkins.ci.plugins.buildtimeblame.io.ReportIO

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
        def previousElapsedTime = 0
        def addSingleMatchIfFound = { String label, String line, Long elapsedMillis ->
            result.add(new ConsoleLogMatch(
                    label: label,
                    matchedLine: line,
                    elapsedMillis: elapsedMillis,
                    previousElapsedTime: previousElapsedTime,
            ))
            previousElapsedTime = elapsedMillis
        }

        processMatches(run, addSingleMatchIfFound)
        ReportIO.getInstance(run).write(result)
        return result
    }

    private void processMatches(Run run, Closure onMatch) {
        long mostRecentTimestamp = 0
        def steps = relevantSteps.collect()
        def reader = TimestamperAPI.get().read(run, 'appendLog&elapsed=S')
        def hasTimestamps = false

        try {
            reader.lines()
                    .map { line -> TimedLog.fromText(line) }
                    .forEach { TimedLog line ->
                        def timestamp = line.elapsedMillis.orElse(mostRecentTimestamp)
                        mostRecentTimestamp = timestamp
                        hasTimestamps = mostRecentTimestamp != 0

                        getMatchingRegex(line.log, steps).ifPresent({ step ->
                            onMatch(step.label, line.log, timestamp)
                        })
                    }
        } finally {
            reader.close()
        }

        if (!hasTimestamps) {
            throw new TimestampMissingException()
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
        return Optional.empty()
    }

    public static class TimestampMissingException extends RuntimeException {
    }
}
