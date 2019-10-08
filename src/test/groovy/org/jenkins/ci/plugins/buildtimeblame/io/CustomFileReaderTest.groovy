//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import spock.lang.Specification

import static CustomFileReader.eachLineOnlyLF

class CustomFileReaderTest extends Specification {
    BufferedInputStream inputStream = null

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

    def 'should close the input stream'() {
        given:
        def mockClosure = Mock(Closure)
        getTestFile().write("sometext")
        inputStream = getTestFile().newInputStream()

        when:
        eachLineOnlyLF(inputStream, mockClosure)
        inputStream.getText()

        then:
        def exception = thrown(IOException)
        exception.message == 'Stream closed'
    }

    def 'should close the input stream even if there is a failure'() {
        given:
        def failureMessage = "Couldn't process item"
        def mockClosure = {
            throw new RuntimeException(failureMessage)
        }
        getTestFile().write("sometext")
        inputStream = getTestFile().newInputStream()

        when:
        eachLineOnlyLF(inputStream, mockClosure)

        then:
        def exception = thrown(RuntimeException)
        exception.message == failureMessage

        when:
        inputStream.getText()

        then:
        def ioException = thrown(IOException)
        ioException.message == 'Stream closed'
    }

    private static File getTestFile() {
        new File('templog.txt')
    }
}
