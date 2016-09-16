//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import org.jenkins.ci.plugins.buildtimeblame.analysis.RelevantStep
import hudson.model.AbstractProject
import net.sf.json.JSONArray
import net.sf.json.JSONObject

import static BlameFilePaths.getConfigFile

class ConfigIO {
    AbstractProject project

    ConfigIO(AbstractProject project) {
        this.project = project
    }

    public void write(List<RelevantStep> relevantSteps) {
        getConfigFile(project).write(convertToString(relevantSteps))
    }

    public List<RelevantStep> readOrDefault(List<RelevantStep> defaultValue = []) {
        def file = getConfigFile(project)

        try {
            readValue(JSONArray.fromObject(file.text).collect() as List<JSONObject>)
        } catch (Exception ignored) {
            return defaultValue
        }
    }

    public static List<RelevantStep> readValue(List<JSONObject> objects) {
        objects.collect { Map<String, String> step ->
            new RelevantStep(~step.key, step.label, Boolean.valueOf(step.onlyFirstMatch))
        }
    }

    private static String convertToString(List<RelevantStep> relevantSteps) {
        JSONArray.fromObject(relevantSteps.collect { RelevantStep entry ->
            [key: entry.pattern.pattern(), label: entry.label, onlyFirstMatch: entry.onlyFirstMatch]
        }).toString()
    }
}
