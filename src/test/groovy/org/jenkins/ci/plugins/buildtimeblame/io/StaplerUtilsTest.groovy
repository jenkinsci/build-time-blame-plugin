//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import net.sf.json.JSONObject
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse
import spock.lang.Specification

class StaplerUtilsTest extends Specification {
    def 'should redirect to parent url'() {
        given:
        def fullUrl = 'http://anywhere.com/jenkins/job/someJob/someTask/currentAction'
        def request = Mock(StaplerRequest)
        def response = Mock(StaplerResponse)

        when:
        StaplerUtils.redirectToParentURI(request, response)

        then:
        1 * request.getOriginalRequestURI() >> fullUrl
        1 * response.sendRedirect('http://anywhere.com/jenkins/job/someJob/someTask')
    }

    def 'should redirect to parent url if there is a trailing /'() {
        given:
        def fullUrl = 'http://anywhere.com/jenkins/job/otherTask/otherAction/'
        def request = Mock(StaplerRequest)
        def response = Mock(StaplerResponse)

        when:
        StaplerUtils.redirectToParentURI(request, response)

        then:
        1 * request.getOriginalRequestURI() >> fullUrl
        1 * response.sendRedirect('http://anywhere.com/jenkins/job/otherTask')
    }

    def 'should return list value if it is already a list'() {
        given:
        def baseObject = JSONObject.fromObject([one: ['first', 'second', 'third']])

        when:
        def result = StaplerUtils.getAsList(baseObject, 'one') as List<String>

        then:
        result == ['first', 'second', 'third']
    }

    def 'should return list value if it is a value'() {
        given:
        def baseObject = JSONObject.fromObject([fifty: 'first value'])

        when:
        def result = StaplerUtils.getAsList(baseObject, 'fifty') as List<String>

        then:
        result == ['first value']
    }

    def 'should return list value if it is a map'() {
        given:
        def baseObject = JSONObject.fromObject([two: [first: 'value']])

        when:
        def result = StaplerUtils.getAsList(baseObject, 'two')

        then:
        result[0].toMapString() == [first: 'value'].toMapString()
    }
}
