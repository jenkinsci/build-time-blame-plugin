//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.action

import hudson.model.AbstractProject
import hudson.model.TransientProjectActionFactory
import spock.lang.Specification

class BlameActionFactoryTest extends Specification {
    def 'should add action to all projects'() {
        given:
        def project = Mock(AbstractProject) {
            it.rootDir >> new File('')
        }
        TransientProjectActionFactory factory = new BlameActionFactory()

        when:
        def addedActions = factory.createFor(project)

        then:
        addedActions == [new BlameAction(project)]
    }
}
