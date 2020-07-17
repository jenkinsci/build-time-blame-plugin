//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import hudson.model.Job
import org.jenkins.ci.plugins.buildtimeblame.analysis.RelevantStep

import static org.jenkins.ci.plugins.buildtimeblame.io.BlameFilePaths.getConfigFile
import static org.jenkins.ci.plugins.buildtimeblame.io.BlameFilePaths.getLegacyConfigFile


class ConfigIO {
    Job job
    ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    ConfigIO(Job job) {
        this.job = job
    }

    public ReportConfiguration parse(String configString) {
        objectMapper.readValue(configString, ReportConfiguration)
    }

    public write(ReportConfiguration reportConfig) {
        getConfigFile(job).write(objectMapper.writeValueAsString(reportConfig))
    }

    public ReportConfiguration readOrDefault(List<RelevantStep> defaultSteps = []) {
        try {
            ReportConfiguration configuration = getConfiguration()
            return configuration
        } catch (Exception ignored) {
            return new ReportConfiguration(relevantSteps: defaultSteps)
        }
    }

    private ReportConfiguration getConfiguration() {
        def legacyFile = getLegacyConfigFile(job)

        if (legacyFile.exists()) {
            def relevantSteps = objectMapper.readValue(legacyFile.text, RelevantStep[].class)
            def configuration = new ReportConfiguration(relevantSteps: relevantSteps)
            moveConfigToNewFile(configuration, legacyFile)
            return configuration
        }

        return parse(getConfigFile(job).text)
    }

    private void moveConfigToNewFile(ReportConfiguration configuration, File legacyFile) {
        getConfigFile(job).write(objectMapper.writeValueAsString(configuration))
        legacyFile.delete()
    }
}
