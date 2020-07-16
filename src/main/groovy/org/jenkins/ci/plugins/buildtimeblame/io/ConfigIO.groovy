//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import hudson.model.Job
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.jenkins.ci.plugins.buildtimeblame.analysis.RelevantStep

import static BlameFilePaths.getConfigFile

class ConfigIO {
    Job job

    ConfigIO(Job job) {
        this.job = job
    }

    public void write(List<RelevantStep> relevantSteps) {
        getConfigFile(job).write(convertToString(relevantSteps))
    }

    public List<RelevantStep> readOrDefault(List<RelevantStep> defaultValue = []) {
        def file = getConfigFile(job)

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
