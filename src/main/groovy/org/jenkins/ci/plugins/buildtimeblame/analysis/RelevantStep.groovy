//  Copyright (c) 2016 Deere & Company

package org.jenkins.ci.plugins.buildtimeblame.analysis

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import java.util.regex.Pattern

@EqualsAndHashCode
@ToString
class RelevantStep {
    @JsonFormat(shape= JsonFormat.Shape.OBJECT)
    @JsonProperty("key")
    Pattern pattern
    String label
    boolean onlyFirstMatch

    RelevantStep() {
    }

    RelevantStep(Pattern pattern, String label, Boolean onlyFirstMatch) {
        this.pattern = pattern
        this.label = label
        this.onlyFirstMatch = onlyFirstMatch
    }
}
