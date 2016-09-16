//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.AutoClone
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import hudson.plugins.timestamper.Timestamp
import org.apache.commons.lang.time.DurationFormatUtils

@AutoClone
@EqualsAndHashCode
@ToString(includeNames = true)
class ConsoleLogMatch {
    private static final int TARGETED_LINE_LENGTH = 30

    String label
    String matchedLine
    Timestamp timestamp
    long previousElapsedTime

    String getElapsedTime() {
        format(timestamp.elapsedMillis)
    }

    String getTimeTaken() {
        format(getUnFormattedTimeTaken())
    }

    long getUnFormattedTimeTaken() {
        timestamp.elapsedMillis - previousElapsedTime
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
