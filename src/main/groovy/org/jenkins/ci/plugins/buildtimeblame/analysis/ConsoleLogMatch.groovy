//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.AutoClone
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

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
        return formatElapsed(elapsedMillis);
    }
    /**
     * Formats a duration as mm:ss.SSS, matching what Commons Lang's
     * DurationFormatUtils.formatDuration(millis, 'mm:ss.S') produced -- note the milliseconds are
     * zero-padded to three digits, and the minutes accumulate rather than rolling over at 60.
     */
    private static String formatElapsed(long elapsedMillis) {
        return String.format('%02d:%02d.%03d',
                elapsedMillis.intdiv(60000),
                elapsedMillis.intdiv(1000) % 60,
                elapsedMillis % 1000)
    }

}
