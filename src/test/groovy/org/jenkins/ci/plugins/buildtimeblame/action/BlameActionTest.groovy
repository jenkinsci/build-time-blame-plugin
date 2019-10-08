//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.action

import groovy.transform.ToString
import hudson.model.Action
import hudson.model.Job
import hudson.model.Result
import hudson.model.Run
import hudson.util.RunList
import org.jenkins.ci.plugins.buildtimeblame.analysis.*
import org.jenkins.ci.plugins.buildtimeblame.io.ConfigIO
import org.jenkins.ci.plugins.buildtimeblame.io.ReportIO
import org.jenkins.ci.plugins.buildtimeblame.io.StaplerUtils
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse
import spock.lang.Specification

import static net.sf.json.JSONObject.fromObject

class BlameActionTest extends Specification {
    ConfigIO mockConfigIO

    void setup() {
        GroovyMock(StaplerUtils, global: true)
        mockConfigIO = GroovyMock(ConfigIO, global: true)
        //noinspection GroovyAssignabilityCheck
        _ * new ConfigIO(_) >> mockConfigIO
        _ * mockConfigIO.readOrDefault(_ as List<RelevantStep>) >> [new RelevantStep(~'.*', 'label', true)]
    }

    def 'should implement basic Action methods'() {
        when:
        Action blameAction = new BlameAction(null);

        then:
        blameAction.getIconFileName() == 'monitor.png'
        blameAction.getDisplayName() == 'Build Time Blame Report'
        blameAction.getUrlName() == 'buildTimeBlameReport'
    }

    def 'should store job'() {
        given:
        Job job = Mock(Job)

        when:
        def blameAction = new BlameAction(job)

        then:
        blameAction.job == job
    }

    def 'should read configuration from file'() {
        given:
        def job = Mock(Job)
        def expected = [new RelevantStep((~'.*anything.*'), 'label', true)]

        when:
        def blameAction = new BlameAction(job)

        then:
        1 * new ConfigIO(job) >> mockConfigIO
        1 * mockConfigIO.readOrDefault(BlameAction.DEFAULT_PATTERNS) >> expected
        blameAction.relevantSteps == expected
    }

    def 'should have correct default patterns'() {
        expect:
        BlameAction.DEFAULT_PATTERNS.toListString() == [
                new RelevantStep(~/.*Started by.*/, 'Job Started on Executor', true),
                new RelevantStep(~/^Finished: (SUCCESS|UNSTABLE|FAILURE|NOT_BUILT|ABORTED)$.*/, 'Finished', true),
        ].toListString()
    }

    def 'should parse log for each build'() {
        given:
        def lastBuildNumber = 37
        def logParser = GroovyMock(LogParser, global: true)
        def job = Mock(Job)
        def build1 = getRunWith(Result.SUCCESS)
        _ * build1.number >> lastBuildNumber
        def build2 = getRunWith(Result.SUCCESS)
        _ * build1.number >> 36
        def build1Results = new BuildResult(consoleLogMatches: [new ConsoleLogMatch(label: 'one')])
        def build2Results = new BuildResult(consoleLogMatches: [new ConsoleLogMatch(label: 'two')])
        def blameAction = new BlameAction(job)

        when:
        def report = blameAction.getReport()

        then:
        1 * job.getBuilds() >> RunList.fromRuns([build1, build2])
        1 * new LogParser(blameAction.relevantSteps) >> logParser
        1 * logParser.getBuildResult(build1) >> build1Results
        1 * logParser.getBuildResult(build2) >> build2Results
        report == new BlameReport([build1Results, build2Results])
        blameAction.buildsWithoutTimestamps == []
        blameAction.lastProcessedBuild == lastBuildNumber
    }

    def 'should only include successful or unstable builds'() {
        given:
        def logParser = GroovyMock(LogParser, global: true)
        def job = Mock(Job)
        def successful = getRunWith(Result.SUCCESS)
        def unstable = getRunWith(Result.UNSTABLE)
        def failed = getRunWith(Result.FAILURE)
        def aborted = getRunWith(Result.ABORTED)
        def notBuilt = getRunWith(Result.NOT_BUILT)
        def blameAction = new BlameAction(job)

        when:
        blameAction.getReport()

        then:
        1 * job.getBuilds() >> RunList.fromRuns([successful, unstable, failed, aborted, notBuilt])
        1 * new LogParser(blameAction.relevantSteps) >> logParser
        1 * logParser.getBuildResult(successful) >> []
        1 * logParser.getBuildResult(unstable) >> []
        0 * logParser.getBuildResult(_ as Run)
    }

    def 'should only include non-running builds'() {
        given:
        def logParser = GroovyMock(LogParser, global: true)
        def job = Mock(Job)
        def running = getRunWith(Result.SUCCESS, true)
        def notRunning = getRunWith(Result.UNSTABLE)
        def blameAction = new BlameAction(job)

        when:
        blameAction.getReport()

        then:
        1 * job.getBuilds() >> RunList.fromRuns([running, notRunning])
        1 * new LogParser(blameAction.relevantSteps) >> logParser
        1 * logParser.getBuildResult(notRunning) >> []
        0 * logParser.getBuildResult(_ as Run)
    }

    def 'should handle missing timestamps'() {
        given:
        def lastBuildNumber = 6
        def logParser = GroovyMock(LogParser, global: true)
        def job = Mock(Job)
        Run build1 = getRunWith(Result.SUCCESS)
        _ * build1.number >> 5
        Run build2 = getRunWith(Result.SUCCESS)
        _ * build2.number >> lastBuildNumber
        def build1Results = new BuildResult(consoleLogMatches: [new ConsoleLogMatch(label: 'one')])
        def blameAction = new BlameAction(job)

        when:
        def report = blameAction.getReport()

        then:
        1 * job.getBuilds() >> RunList.fromRuns([build1, build2])
        1 * new LogParser(blameAction.relevantSteps) >> logParser
        1 * logParser.getBuildResult(build1) >> build1Results
        1 * logParser.getBuildResult(build2) >> { throw new RuntimeException() }
        report == new BlameReport([build1Results])
        blameAction.buildsWithoutTimestamps == [build2]
        blameAction.lastProcessedBuild == lastBuildNumber
    }

    def 'should override equal appropriately'() {
        given:
        def job = Mock(Job)

        expect:
        new BlameAction(job) == new BlameAction(job)
        new BlameAction(job) != new BlameAction(Mock(Job))
        new BlameAction(job).each { it.relevantSteps = [] } != new BlameAction(job)
    }

    def 'should update configuration'() {
        given:
        def job = Mock(Job)
        def response = Mock(StaplerResponse)
        def blameAction = new BlameAction(job)
        def originalObject = fromObject([key: 'value'])
        def expectedRelevantSteps = [new RelevantStep(~/.*|.*/, 'anything happened', false)]
        def submittedSteps = [fromObject([key: 'value'])]
        def request = Mock(StaplerRequest) { _ * it.getSubmittedForm() >> originalObject }

        when:
        blameAction.doReprocessBlameReport(request, response)

        then:
        1 * StaplerUtils.getAsList(originalObject, 'relevantSteps') >> submittedSteps
        1 * new ConfigIO(job) >> mockConfigIO
        1 * mockConfigIO.readValue(submittedSteps) >> expectedRelevantSteps
        1 * mockConfigIO.write(expectedRelevantSteps)
        blameAction.relevantSteps == expectedRelevantSteps
    }

    def 'should redirect to parent when updating configuration'() {
        given:
        def response = Mock(StaplerResponse)
        def request = Mock(StaplerRequest)
        def blameAction = new BlameAction(Mock(Job))

        when:
        blameAction.doReprocessBlameReport(request, response)

        then:
        1 * StaplerUtils.redirectToParentURI(request, response)
    }

    def 'should clear all build reports when updating configuration'() {
        given:
        def mockReportIO = GroovyMock(ReportIO, global: true)
        def firstRun = getRunWith(Result.FAILURE)
        def secondRun = getRunWith(Result.SUCCESS)

        def job = Mock(Job) { Job job ->
            job.getBuilds() >> RunList.fromRuns([firstRun, secondRun])
        }

        def response = Mock(StaplerResponse)
        def request = Mock(StaplerRequest)
        def blameAction = new BlameAction(job)
        blameAction._report = new BlameReport(null)
        blameAction.buildsWithoutTimestamps = [Mock(Run)]

        when:
        blameAction.doReprocessBlameReport(request, response)

        then:
        1 * new ReportIO(firstRun) >> mockReportIO
        1 * new ReportIO(secondRun) >> mockReportIO
        2 * mockReportIO.clear()
        blameAction._report == null
        blameAction.buildsWithoutTimestamps == []
    }

    def 'should not recalculate build results if report exists'() {
        given:
        def job = Mock(Job)
        def blameAction = new BlameAction(job)
        def expected = new BlameReport([])
        def runWithoutTimestamps = Mock(Run)
        def lastBuildNumber = 3
        blameAction._report = expected
        blameAction.buildsWithoutTimestamps = [runWithoutTimestamps]
        blameAction.lastProcessedBuild = lastBuildNumber

        when:
        def report = blameAction.getReport()

        then:
        report == expected
        blameAction.buildsWithoutTimestamps == [runWithoutTimestamps]
        1 * job.getNearestBuild(3) >> null
        0 * _
    }

    def 'should recalculate build results if new builds have been run'() {
        given:
        def job = Mock(Job)
        def blameAction = new BlameAction(job)
        def expected = new BlameReport([])
        def runWithoutTimestamps = Mock(Run)
        def lastBuildNumber = 15
        blameAction._report = expected
        blameAction.buildsWithoutTimestamps = [runWithoutTimestamps]
        blameAction.lastProcessedBuild = lastBuildNumber
        def logParser = GroovyMock(LogParser, global: true)
        def build1 = getRunWith(Result.SUCCESS)
        def build2 = getRunWith(Result.SUCCESS)
        def build1Results = new BuildResult(consoleLogMatches: [new ConsoleLogMatch(label: 'one')])
        def build2Results = new BuildResult(consoleLogMatches: [new ConsoleLogMatch(label: 'two')])

        when:
        def report = blameAction.getReport()

        then:
        1 * job.getBuilds() >> RunList.fromRuns([build1, build2])
        1 * job.getNearestBuild(lastBuildNumber) >> Mock(hudson.model.AbstractBuild)
        1 * new LogParser(blameAction.relevantSteps) >> logParser
        1 * logParser.getBuildResult(build1) >> build1Results
        1 * logParser.getBuildResult(build2) >> build2Results
        report == new BlameReport([build1Results, build2Results])
    }

    def 'should include helpful annotation'() {
        expect:
        BlameAction.getAnnotation(ToString) != null
    }

    def 'should not have missing timestamp description if no builds'() {
        given:
        def blameAction = new BlameAction(Mock(Job))

        expect:
        blameAction.missingTimestampsDescription == ''
    }

    def 'should not have missing timestamp description if failed builds since the first successful build'() {
        given:
        def blameAction = new BlameAction(Mock(Job))
        blameAction._report = new BlameReport([new BuildResult(build: Mock(Run) { _ * it.getNumber() >> 60 })])
        blameAction.buildsWithoutTimestamps = [
                Mock(Run) { _ * it.getNumber() >> 58 },
                Mock(Run) { _ * it.getNumber() >> 59 },
                Mock(Run) { _ * it.getNumber() >> 57 },
        ]

        expect:
        blameAction.missingTimestampsDescription == ''
    }

    def 'should have correct missing timestamp description if some builds match'() {
        given:
        def blameAction = new BlameAction(Mock(Job))
        blameAction._report = new BlameReport([new BuildResult(build: Mock(Run) { _ * it.getNumber() >> 60 })])
        def firstBuildNumber = 105
        def secondBuildNumber = 99
        def thirdBuildNumber = 61
        def expectedBuildNumbers = "$thirdBuildNumber, $secondBuildNumber, $firstBuildNumber"

        blameAction.buildsWithoutTimestamps = [
                Mock(Run) { _ * it.getNumber() >> 59 },
                Mock(Run) { _ * it.getNumber() >> firstBuildNumber },
                Mock(Run) { _ * it.getNumber() >> secondBuildNumber },
                Mock(Run) { _ * it.getNumber() >> 35 },
                Mock(Run) { _ * it.getNumber() >> thirdBuildNumber },
        ]

        expect:
        blameAction.missingTimestampsDescription == "Error finding timestamps for builds: $expectedBuildNumbers"
    }

    private Run getRunWith(Result result, boolean isBuilding = false) {
        return Mock(Run) {
            _ * it.result >> result
            _ * it.isBuilding() >> isBuilding
        }
    }
}
