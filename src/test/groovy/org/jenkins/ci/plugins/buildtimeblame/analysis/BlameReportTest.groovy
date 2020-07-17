//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import hudson.model.Run
import hudson.plugins.timestamper.Timestamp
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
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 0),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 1000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 25000),
                ]),
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 500),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 2000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 3000),
                ]),
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 100),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 3000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 4000),
                ]),
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 0),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 3000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 5000),
                ]),
        ]

        when:
        def medianBuildResult = new BlameReport(buildResults).getMeanBuildResult()

        then:
        medianBuildResult.size() == 3
        medianBuildResult[0] == buildExpectedMedianResult('Start', 150, 0)
        medianBuildResult[1] == buildExpectedMedianResult('Middle', 2250, 150)
        medianBuildResult[2] == buildExpectedMedianResult('Finish', 9250, 2250)
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
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 0),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 1000),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 25000, previousElapsedTime: 1000),
                ], build: latestBuild),
                new BuildResult(consoleLogMatches: [
                        new ConsoleLogMatch(label: 'Start', elapsedMillis: 500),
                        new ConsoleLogMatch(label: 'Middle', elapsedMillis: 2000, previousElapsedTime: 500),
                        new ConsoleLogMatch(label: 'Finish', elapsedMillis: 3000, previousElapsedTime: 2000),
                ], build: previousBuild),
        ]

        when:
        Graph graph = new BlameReport(buildResults).getGraph()

        then:
        graph.createGraph().getCategoryPlot().getDataset() == getExpectedDataSet()
    }

    def 'should set basic properties of graph'() {
        when:
        Graph graph = new BlameReport([]).getGraph()

        then:
        getFieldValue(Graph, 'defaultW', graph) == 1000
        getFieldValue(Graph, 'defaultH', graph) == 500
        getFieldValue(Graph, 'timestamp', graph) < System.currentTimeMillis()

    }

    def 'should set graph timestamp (this is ugly to avoid including Joda Time or mocking currentTimeMillis())'() {
        given:
        def pauseTime = 15
        def extraPause = 5
        long beforeAll = System.currentTimeMillis()
        sleep(pauseTime + extraPause)
        Graph graph1 = new BlameReport([]).getGraph()
        sleep(pauseTime + extraPause)
        Graph graph2 = new BlameReport([]).getGraph()
        sleep(pauseTime + extraPause)
        long afterAll = System.currentTimeMillis()

        when:
        long timestamp1 = getFieldValue(Graph, 'timestamp', graph1) as long
        long timestamp2 = getFieldValue(Graph, 'timestamp', graph2) as long

        then:
        timestamp1 - beforeAll >= pauseTime
        timestamp2 - beforeAll >= 2 * pauseTime
        afterAll - timestamp1 >= 2 * pauseTime
        afterAll - timestamp2 >= pauseTime
        timestamp2 - timestamp1 >= pauseTime
    }

    CategoryDataset getExpectedDataSet() {
        def dataSet = new DefaultCategoryDataset()

        dataSet.addValue((double) 0.5, 'Start', 53)
        dataSet.addValue((double) 1.5, 'Middle', 53)
        dataSet.addValue((double) 1.0, 'Finish', 53)
        dataSet.addValue((double) 0.0, 'Start', 98)
        dataSet.addValue((double) 1.0, 'Middle', 98)
        dataSet.addValue((double) 24.0, 'Finish', 98)

        return dataSet
    }

    def <T> Object getFieldValue(Class<T> clazz, String fieldName, T instance) {
        def field = ReflectionUtils.findField(clazz, fieldName)
        field.setAccessible(true)
        return ReflectionUtils.getField(field, instance)
    }

    ConsoleLogMatch buildExpectedMedianResult(String label, int elapsedTime, int previousTime) {
        new ConsoleLogMatch(label: label, elapsedMillis: elapsedTime, matchedLine: 'N/A', previousElapsedTime: previousTime)
    }
}
