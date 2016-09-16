//  Copyright (c) 2016 Deere & Company

package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import java.util.regex.Pattern

@EqualsAndHashCode
@ToString
class RelevantStep {
    Pattern pattern
    String label
    boolean onlyFirstMatch

    RelevantStep(Pattern pattern, String label, Boolean onlyFirstMatch) {
        this.pattern = pattern
        this.label = label
        this.onlyFirstMatch = onlyFirstMatch
    }
}
