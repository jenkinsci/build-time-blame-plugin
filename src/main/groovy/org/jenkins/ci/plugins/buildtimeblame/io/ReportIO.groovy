//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import hudson.model.Run
import net.sf.json.JSONObject
import net.sf.json.JsonConfig
import net.sf.json.util.PropertyFilter
import org.jenkins.ci.plugins.buildtimeblame.analysis.ConsoleLogMatch

import static net.sf.json.JSONArray.fromObject
import static org.jenkins.ci.plugins.buildtimeblame.io.BlameFilePaths.getReportFile

class ReportIO {
    Run build

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
        getReportFile(build).write(getJSON(report))
    }

    public Optional<List<ConsoleLogMatch>> readFile() {
        try {
            Optional.of(fromObject(getReportFile(build).text).collect { JSONObject object -> mapToMatch(object) })
        } catch (Exception ignored) {
            return Optional.empty()
        }
    }

    private static String getJSON(List<ConsoleLogMatch> report) {
        fromObject(report, getConfig()).toString()
    }

    private static ConsoleLogMatch mapToMatch(JSONObject object) {
        new ConsoleLogMatch(
                label: object.get('label'),
                matchedLine: object.get('matchedLine'),
                previousElapsedTime: object.get('previousElapsedTime') as long,
                elapsedMillis: object.get('elapsedMillis') as long
        )
    }

    private static JsonConfig getConfig() {
        def jsonConfig = new JsonConfig()
        jsonConfig.setJsonPropertyFilter(new PropertyFilter() {
            @Override
            boolean apply(Object source, String name, Object value) {
                return name.equals('timeTaken') || name.equals('elapsedTime') || name.equals('unFormattedTimeTaken')
            }
        })
        return jsonConfig
    }
}
