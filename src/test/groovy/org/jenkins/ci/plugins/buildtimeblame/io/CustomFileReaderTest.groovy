//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import spock.lang.Specification

import java.util.stream.Collectors

import static CustomFileReader.eachLineOnlyLF

class CustomFileReaderTest extends Specification {
    BufferedInputStream inputStream = null

    void cleanup() {
        inputStream.close()
        getTestFile().delete()
    }

    def 'should call closure for each line separated by LF'() {
        given:
        def line1 = 'The first line\rcontains a carriage return'
        def line2 = 'The second line does not.'
        def line3 = 'What if the third line, \r does too?'
        getTestFile().write("$line1\n$line2\r\n$line3   \r\n")
        inputStream = getTestFile().newInputStream()

        when:
        def result = eachLineOnlyLF(inputStream)

        then:
        result.collect(Collectors.toList()) == [line1, line2, line3]
    }

    private static File getTestFile() {
        new File('templog.txt')
    }
}
