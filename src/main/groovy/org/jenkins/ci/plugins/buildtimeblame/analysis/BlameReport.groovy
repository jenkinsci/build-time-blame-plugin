//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import hudson.util.Graph
import org.apache.commons.lang.time.DurationFormatUtils
import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.CategoryLabelPositions
import org.jfree.chart.labels.CategoryToolTipGenerator
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.urls.CategoryURLGenerator
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
            super(System.currentTimeMillis(), 900, 500)
        }

        @Override
        protected JFreeChart createGraph() {
            return ChartFactory.createStackedAreaChart(
                    '', 'Build #', 'Time Taken (s)', getDataSet(),
                    PlotOrientation.VERTICAL, true, false, false
            ).each {
                def plot = it.getCategoryPlot()
                plot.setRenderer(new BlameStackedAreaRenderer())

                def xAxis = plot.getDomainAxis()
                xAxis.setCategoryMargin(0)
                xAxis.setLowerMargin(0)
                xAxis.setUpperMargin(0)
                xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
            }
        }
    }

    private static class BlameStackedAreaRenderer extends MapFixStackedAreaRenderer
            implements CategoryToolTipGenerator, CategoryURLGenerator {
        BlameStackedAreaRenderer() {
            setItemURLGenerator(this)
            setToolTipGenerator(this)
        }

        @Override
        public String generateToolTip(CategoryDataset dataset, int row, int column) {
            String rowKey = dataset.getRowKey(row)
            int columnKey = dataset.getColumnKey(column)
            double elapsedSeconds = dataset.getValue(rowKey, columnKey)
            long elapsedMillis = 1000.0 * elapsedSeconds
            String duration = DurationFormatUtils.formatDuration(elapsedMillis, 'mm:ss.S')

            return "#${columnKey} ${rowKey}\n${duration} (${(long)elapsedSeconds}s)"
        }

        @Override
        public String generateURL(CategoryDataset dataset, int row, int column) {
            int columnKey = dataset.getColumnKey(column)

            return "../${columnKey}"
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
