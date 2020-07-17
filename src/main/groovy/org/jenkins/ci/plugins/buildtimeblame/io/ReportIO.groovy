//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import com.fasterxml.jackson.databind.ObjectMapper
import hudson.model.Run
import org.jenkins.ci.plugins.buildtimeblame.analysis.ConsoleLogMatch

import static org.jenkins.ci.plugins.buildtimeblame.io.BlameFilePaths.getReportFile

class ReportIO {
    Run build
    private static ObjectMapper objectMapper = new ObjectMapper()

    public static ReportIO getInstance(Run build) {
        return new ReportIO(build)
    }

    ReportIO(Run build) {
        this.build = build
    }

    public void clear() {
        getReportFile(build).delete()
    }

    public void write(List<ConsoleLogMatch> report) {
        getReportFile(build).write(objectMapper.writeValueAsString(report))
    }

    public Optional<List<ConsoleLogMatch>> readFile() {
        try {
            def reportContent = getReportFile(build).text
            def report = objectMapper.readValue(reportContent, ConsoleLogMatch[])

            return Optional.of(Arrays.asList(report))
        } catch (Exception ignored) {
            return Optional.empty()
        }
    }
}
