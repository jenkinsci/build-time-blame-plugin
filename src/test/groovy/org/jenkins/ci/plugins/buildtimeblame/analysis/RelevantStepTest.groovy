//  Copyright (c) 2016 Deere & Company

package org.jenkins.ci.plugins.buildtimeblame.analysis

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import spock.lang.Specification

class RelevantStepTest extends Specification {
    def 'should have helpful annotations'() {
        expect:
        RelevantStep.isAnnotationPresent(EqualsAndHashCode)
        RelevantStep.isAnnotationPresent(ToString)
    }

    def 'should provide constructor for all properties'() {
        given:
        def pattern = ~/.*/
        def label = 'Anything happened'
        def onlyFirstMatch = true

        when:
        def step = new RelevantStep(pattern, label, onlyFirstMatch)

        then:
        step.pattern == pattern
        step.label == label
        step.onlyFirstMatch == onlyFirstMatch
    }

    def 'should handle null onlyFirstMatch in constructor with false as default'() {
        given:
        def pattern = ~/.*/
        def label = 'Anything happened'

        when:
        def step = new RelevantStep(pattern, label, null)

        then:
        step.pattern == pattern
        step.label == label
        !step.onlyFirstMatch
    }
}
