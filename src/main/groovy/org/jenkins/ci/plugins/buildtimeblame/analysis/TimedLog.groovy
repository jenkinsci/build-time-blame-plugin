package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.EqualsAndHashCode
import org.apache.commons.lang3.StringUtils

import java.util.stream.Collectors

@EqualsAndHashCode
class TimedLog {
    String log
    Optional<Long> elapsedMillis = Optional.empty()

    public static TimedLog fromText(String timestamperLog) {
        if (timestamperLog.startsWith(' ')) {
            return new TimedLog(
                    log: timestamperLog.trim()
            )
        } else {
            def split = timestamperLog.split(' ')
            if (StringUtils.isNumeric(split[0])) {
                return new TimedLog(
                    elapsedMillis: Optional.of(Long.valueOf(split[0])),
                    log: Arrays.stream(split).skip(1).collect(Collectors.joining(' ')).trim(),
                )
            } else {
                return new TimedLog(
                        log: timestamperLog.trim()
                )
            }
        }
    }

    String toText() {
        def elapsed = elapsedMillis.map({ time -> String.valueOf(time) }).orElse(' ')

        return "$elapsed $log"
    }
}
