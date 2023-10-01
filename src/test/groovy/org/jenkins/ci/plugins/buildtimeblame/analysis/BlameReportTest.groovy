//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import hudson.model.Run
import hudson.util.Graph
import hudson.util.ReflectionUtils
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset
import spock.lang.Specification

class BlameReportTest extends Specification {
    def 'should include helpful annotations'() {
        expect:
        BlameReport.getAnnotation(EqualsAndHashCode) != null
        BlameReport.getAnnotation(ToString) != null
    }

    def 'should get latest build result'() {
        given:
        def logResult = new ConsoleLogMatch(label: 'test')
        def buildResults = [new BuildResult(consoleLogMatches: [logResult]), null, null]

        when:
        def firstBuildResult = new BlameReport(buildResults).getLatestBuildResult()

        then:
        firstBuildResult == [logResult]
    }

    def 'should handle no builds on latest build result'() {
        given:
        def buildResults = []

        when:
        def firstBuildResult = new BlameReport(buildResults).getLatestBuildResult()

        then:
        firstBuildResult == []
    }

    def 'should get mean build result'() {
        given:
        def buildResults = [
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 0, elapsedMillisOfNextMatch: 1000),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 1000, elapsedMillisOfNextMatch: 25000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 25000, elapsedMillisOfNextMatch: 26000),
                ]),
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 500, elapsedMillisOfNextMatch: 2000),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 2000, elapsedMillisOfNextMatch: 3000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 3000, elapsedMillisOfNextMatch: 5000)
                ]),
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 100, elapsedMillisOfNextMatch: 3000),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 3000, elapsedMillisOfNextMatch: 4000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 4000, elapsedMillisOfNextMatch: 10000),
                ]),
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 0, elapsedMillisOfNextMatch: 3000),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 3000, elapsedMillisOfNextMatch: 5000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 5000, elapsedMillisOfNextMatch: 7000),
                ]),
        ]

        when:
        def medianBuildResult = new BlameReport(buildResults).getMeanBuildResult()

        then:
        medianBuildResult.size() == 3
        medianBuildResult[0] == buildExpectedMedianResult('Start', 150, 2250)
        medianBuildResult[1] == buildExpectedMedianResult('Middle', 2250, 9250)
        medianBuildResult[2] == buildExpectedMedianResult('Finish', 9250, 12000)
    }

    def 'should cache mean build result'() {
        given:
        def blameReport = new BlameReport([])
        def expected = [buildExpectedMedianResult('Starting', 150, 0)]
        blameReport._meanBuildResult = expected

        when:
        def meanBuildResult = blameReport.getMeanBuildResult()

        then:
        meanBuildResult == expected
    }

    def 'should build graph of all build results over time'() {
        given:
        def latestBuildNumber = 98
        def previousBuildNumber = 53
        def latestBuild = Mock(Run) { Run it ->
            _ * it.getNumber() >> latestBuildNumber
        }
        def previousBuild = Mock(Run) { Run it ->
            _ * it.getNumber() >> previousBuildNumber
        }

        def buildResults = [
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 0, elapsedMillisOfNextMatch: 1000),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 1000, elapsedMillisOfNextMatch: 25000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 25000, elapsedMillisOfNextMatch: 26000),
                ], build: latestBuild),
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 500, elapsedMillisOfNextMatch: 2000),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 2000, elapsedMillisOfNextMatch: 3000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 3000, elapsedMillisOfNextMatch: 5000)
                ], build: previousBuild),
        ]

        when:
        Graph graph = new BlameReport(buildResults).getGraph()

        then:
        graph.createGraph().getCategoryPlot().getDataset() == getExpectedDataSet()
    }

    def 'should build graph with tooltip and URL generators'() {
        given:
        def report = new BlameReport([])
        def graph = report.getGraph()

        when:
        def chart = graph.createGraph()

        then:
        verifyAll(chart.getCategoryPlot().getRenderer()) {
            getToolTipGenerator(0, 0) != null
            getItemURLGenerator(0, 0) != null
        }
    }

    def 'should generate tooltips for graph areas'() {
        given:
        def buildNumber = 98
        def buildResults = [
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 0, elapsedMillisOfNextMatch: 1000),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 1000, elapsedMillisOfNextMatch: 124000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 124000, elapsedMillisOfNextMatch: 260000),
                ], build: Mock(Run) { _ * it.getNumber() >> buildNumber }),
        ]
        def report = new BlameReport(buildResults)
        def dataSet = report.getDataSet()
        def chart = report.getGraph().createGraph()
        def toolTipGenerator = chart.getCategoryPlot().getRenderer().getToolTipGenerator(1, 0)

        when:
        def toolTip = toolTipGenerator.generateToolTip(dataSet, 1, 0)

        then:
        toolTip != null
        toolTip.contains("${buildNumber}")
        toolTip.contains('Middle')
        toolTip.contains('123')
    }

    def 'should generate URLs for graph areas'() {
        given:
        def buildNumber = 98
        def buildResults = [
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 0, elapsedMillisOfNextMatch: 1000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 1000, elapsedMillisOfNextMatch: 1100),
                ], build: Mock(Run) { _ * it.getNumber() >> buildNumber }),
        ]
        def report = new BlameReport(buildResults)
        def dataSet = report.getDataSet()
        def chart = report.getGraph().createGraph()
        def urlGenerator = chart.getCategoryPlot().getRenderer().getItemURLGenerator(1, 0)

        when:
        def url = urlGenerator.generateURL(dataSet, 1, 0)

        then:
        url != null
        url.contains("${buildNumber}")
    }

    CategoryDataset getExpectedDataSet() {
        def dataSet = new DefaultCategoryDataset()

        dataSet.addValue((double) 1.5, 'Start', 53)
        dataSet.addValue((double) 1.0, 'Middle', 53)
        dataSet.addValue((double) 2.0, 'Finish', 53)
        dataSet.addValue((double) 1.0, 'Start', 98)
        dataSet.addValue((double) 24.0, 'Middle', 98)
        dataSet.addValue((double) 1.0, 'Finish', 98)

        return dataSet
    }

    def <T> Object getFieldValue(Class<T> clazz, String fieldName, T instance) {
        def field = ReflectionUtils.findField(clazz, fieldName)
        field.setAccessible(true)
        return ReflectionUtils.getField(field, instance)
    }

    long getTimestamp(Graph graph) {
        return (graph as BlameReport.GraphImpl).getTimestamp()
    }

    ConsoleLogMatch buildExpectedMedianResult(String label, int elapsedTime, int nextTime) {
        new ConsoleLogMatch(label: label, elapsedMillis: elapsedTime, matchedLine: 'N/A', elapsedMillisOfNextMatch: nextTime)
    }
}
