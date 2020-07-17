//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import hudson.util.Graph
import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset

@EqualsAndHashCode
@ToString(includeNames = true)
class BlameReport {
    List<BuildResult> buildResults
    List<ConsoleLogMatch> _meanBuildResult

    BlameReport(List<BuildResult> buildResults) {
        this.buildResults = buildResults
    }

    List<ConsoleLogMatch> getLatestBuildResult() {
        if (this.buildResults.isEmpty()) {
            return []
        }

        return this.buildResults[0].consoleLogMatches
    }

    List<ConsoleLogMatch> getMeanBuildResult() {
        if (_meanBuildResult == null) {
            _meanBuildResult = calculateMeanBuildResult()
        }

        return _meanBuildResult
    }

    Graph getGraph() {
        return new GraphImpl()
    }

    public class GraphImpl extends Graph {
        protected GraphImpl() {
            super(System.currentTimeMillis(), 1000, 500)
        }

        @Override
        protected JFreeChart createGraph() {
            return ChartFactory.createStackedAreaChart(
                    '', 'Build #', 'Time Taken (s)', getDataSet(),
                    PlotOrientation.VERTICAL, true, false, false
            ).each {
                it.getCategoryPlot().getDomainAxis().setCategoryMargin(0)
            }
        }
    }

    private CategoryDataset getDataSet() {
        def dataSet = new DefaultCategoryDataset()
        for (BuildResult buildResult : buildResults.reverse()) {
            def buildNumber = buildResult.build.getNumber()
            for (ConsoleLogMatch match : buildResult.consoleLogMatches) {
                dataSet.addValue(getTimeTakenInSeconds(match), match.label, buildNumber)
            }
        }
        return dataSet
    }

    private static double getTimeTakenInSeconds(ConsoleLogMatch match) {
        match.unFormattedTimeTaken / 1000
    }

    private List<ConsoleLogMatch> calculateMeanBuildResult() {
        List<ConsoleLogMatch> meanBuildResult = []
        Multimap<String, ConsoleLogMatch> allBuildResults = getAllBuildResults()

        for (String label : allBuildResults.keySet()) {
            def meanElapsedMillis = mean(allBuildResults.get(label).elapsedMillis as List<Long>)
            def meanElapsedMillisOfNextMatch = mean(allBuildResults.get(label).elapsedMillisOfNextMatch as List<Long>)

            meanBuildResult.add(new ConsoleLogMatch(
                    label: label,
                    elapsedMillis: meanElapsedMillis,
                    matchedLine: 'N/A',
                    elapsedMillisOfNextMatch: meanElapsedMillisOfNextMatch,
            ))
        }
        return meanBuildResult
    }

    private static long mean(List<Long> values) {
        return values.sum() / values.size()
    }

    private Multimap<String, ConsoleLogMatch> getAllBuildResults() {
        Multimap<String, ConsoleLogMatch> allBuildResults = LinkedListMultimap.create()
        for (BuildResult buildResult : buildResults) {
            for (ConsoleLogMatch logResult : buildResult.consoleLogMatches) {
                allBuildResults.get(logResult.label).add(logResult)
            }
        }
        return allBuildResults
    }
}
