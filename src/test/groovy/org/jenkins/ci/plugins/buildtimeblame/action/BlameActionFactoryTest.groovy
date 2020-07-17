//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.action

import hudson.model.Job
import jenkins.model.TransientActionFactory
import spock.lang.Specification

class BlameActionFactoryTest extends Specification {
    def 'should add action to all jobs'() {
        given:
        def job = Mock(Job) {
            it.rootDir >> new File('')
        }
        TransientActionFactory<Job> factory = new BlameActionFactory()

        when:
        def addedActions = factory.createFor(job)

        then:
        addedActions == [new BlameAction(job)]
    }

    def 'should apply to all jobs'() {
        given:
        TransientActionFactory<Job> factory = new BlameActionFactory()

        when:
        def type = factory.type()

        then:
        type == Job
    }
}
