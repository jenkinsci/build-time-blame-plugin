package org.jenkins.ci.plugins.buildtimeblame.io

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.jenkins.ci.plugins.buildtimeblame.analysis.RelevantStep

@ToString
@EqualsAndHashCode(includeFields=true)
class ReportConfiguration {
    Integer maxBuilds
    List<RelevantStep> relevantSteps
}
