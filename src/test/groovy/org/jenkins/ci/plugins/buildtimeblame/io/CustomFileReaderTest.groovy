//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import spock.lang.Specification

import static CustomFileReader.eachLineOnlyLF

class CustomFileReaderTest extends Specification {
    InputStream inputStream = null

    void cleanup() {
        inputStream.close()
        getTestFile().delete()
    }

    def 'should call closure for each line separated by LF'() {
        given:
        def mockClosure = Mock(Closure)
        def line1 = 'The first line\rcontains a carriage return'
        def line2 = 'The second line does not.'
        def line3 = 'What if the third line, \r does too?'
        getTestFile().write("$line1\n$line2\r\n$line3   \r\n")
        inputStream = getTestFile().newInputStream()

        when:
        eachLineOnlyLF(inputStream, mockClosure)

        then:
        1 * mockClosure.call(line1)
        1 * mockClosure.call(line2)
        1 * mockClosure.call(line3)
        0 * mockClosure.call(_)
    }

    private static File getTestFile() {
        new File('templog.txt')
    }
}
