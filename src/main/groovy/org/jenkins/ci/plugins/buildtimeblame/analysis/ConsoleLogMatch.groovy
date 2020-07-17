//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.AutoClone
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.commons.lang.time.DurationFormatUtils

import java.beans.Transient

@AutoClone
@EqualsAndHashCode
@ToString(includeNames = true)
class ConsoleLogMatch {
    private static final int TARGETED_LINE_LENGTH = 30

    String label
    String matchedLine
    long elapsedMillis
    long elapsedMillisOfNextMatch

    @Transient
    String getElapsedTime() {
        format(elapsedMillis)
    }

    @Transient
    String getTimeTaken() {
        format(getUnFormattedTimeTaken())
    }

    @Transient
    long getUnFormattedTimeTaken() {
        elapsedMillisOfNextMatch - elapsedMillis
    }

    String getMatchedLine() {
        if (matchedLine.length() > TARGETED_LINE_LENGTH + 3) {
            return matchedLine.substring(0, TARGETED_LINE_LENGTH) + '...'
        }

        return matchedLine
    }

    private static String format(long elapsedMillis) {
        return DurationFormatUtils.formatDuration(elapsedMillis, 'mm:ss.S');
    }
}
