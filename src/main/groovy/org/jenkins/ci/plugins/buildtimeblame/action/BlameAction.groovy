//  Copyright (c) 2016 Deere & Company

package org.jenkins.ci.plugins.buildtimeblame.action

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import hudson.model.Action
import hudson.model.Job
import hudson.model.Result
import hudson.model.Run
import net.sf.json.JSONObject
import org.jenkins.ci.plugins.buildtimeblame.analysis.BlameReport
import org.jenkins.ci.plugins.buildtimeblame.analysis.BuildResult
import org.jenkins.ci.plugins.buildtimeblame.analysis.LogParser
import org.jenkins.ci.plugins.buildtimeblame.analysis.RelevantStep
import org.jenkins.ci.plugins.buildtimeblame.io.ConfigIO
import org.jenkins.ci.plugins.buildtimeblame.io.ReportConfiguration
import org.jenkins.ci.plugins.buildtimeblame.io.ReportIO
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse

import java.util.stream.Collectors

import static org.jenkins.ci.plugins.buildtimeblame.io.StaplerUtils.redirectToParentURI

@ToString(includeNames = true)
@EqualsAndHashCode
class BlameAction implements Action {
    static final List<RelevantStep> DEFAULT_PATTERNS = [
            new RelevantStep(~/.*Started by.*/, 'Job Started on Executor', true),
            new RelevantStep(~/^Finished: (SUCCESS|UNSTABLE|FAILURE|NOT_BUILT|ABORTED)$.*/, 'Finished', true),
    ]

    Job job
    List<Run> buildsWithoutTimestamps = []
    ReportConfiguration config
    private BlameReport _report
    private int lastProcessedBuild

    BlameAction(Job job) {
        this.job = job
        this.config = new ConfigIO(job).readOrDefault(DEFAULT_PATTERNS)
    }

    @Override
    String getIconFileName() {
        return 'monitor.png'
    }

    @Override
    String getDisplayName() {
        return 'Build Time Blame Report'
    }

    @Override
    String getUrlName() {
        return 'buildTimeBlameReport'
    }

    String getMissingTimestampsDescription() {
        if (buildsWithoutTimestamps.isEmpty()) {
            return ''
        }

        List<Integer> buildNumbers = getFailedBuildNumbers()
        if (buildNumbers.isEmpty()) {
            return ''
        }

        return "Error finding timestamps for builds: ${buildNumbers.sort().join(', ')}"
    }

    BlameReport getReport() {
        if (_report == null || hasNewBuilds()) {
            buildsWithoutTimestamps = []
            _report = new BlameReport(getBuildResults(new LogParser(this.config.relevantSteps)))
        }

        return _report
    }

    private boolean hasNewBuilds() {
        job.getNearestBuild(lastProcessedBuild) != null
    }

    public doReprocessBlameReport(StaplerRequest request, StaplerResponse response) {
        updateConfiguration(request.getSubmittedForm())
        clearReports()
        redirectToParentURI(request, response)
    }

    private List<Integer> getFailedBuildNumbers() {
        def firstSuccessful = report.buildResults.collect({ it.build.getNumber() }).min()

        return buildsWithoutTimestamps
                .collect({ it.getNumber() })
                .findAll({ it > firstSuccessful })
    }

    private void clearReports() {
        job.getBuilds().each { Run build -> new ReportIO(build).clear() }
        _report = null
        buildsWithoutTimestamps = []
    }

    private void updateConfiguration(JSONObject jsonObject) {
        def configIO = new ConfigIO(job)
        config = configIO.parse(jsonObject.toString())
        configIO.write(config)
    }

    private List<BuildResult> getBuildResults(LogParser logParser) {
        return job.getBuilds().stream()
                .filter({ Run run ->
                    return !run.isBuilding() && run.result.isBetterOrEqualTo(Result.UNSTABLE)
                })
                .limit(config.maxBuilds != null && config.maxBuilds > 0 ? config.maxBuilds : Integer.MAX_VALUE)
                .map({ Run run ->
                    try {
                        return logParser.getBuildResult(run)
                    } catch (RuntimeException ignored) {
                        buildsWithoutTimestamps.add(run)
                        return null
                    } finally {
                        lastProcessedBuild = Math.max(lastProcessedBuild, run.getNumber())
                    }
                })
                .filter({
                    it != null
                })
                .collect(Collectors.toList()) as List<BuildResult>
    }
}
