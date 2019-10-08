//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io


import hudson.model.Job
import hudson.model.Run
import spock.lang.Specification

class BlameFilePathsTest extends Specification {
    def 'should find correct job configuration file'() {
        given:
        def rootDir = 'Z:\\JenkinsRoot\\job\\JobRoot'
        def job = Mock(Job) {
            _ * it.getRootDir() >> new File(rootDir)
        }

        when:
        File file = BlameFilePaths.getConfigFile(job)

        then:
        file.path == new File(rootDir, 'buildtimeblameconfig').path
    }

    def 'should find correct build data file'() {
        given:
        def rootDir = 'X:\\MyJenkinsRoot\\job\\MyJobRoot\\99'
        def build = Mock(Run) {
            _ * it.getRootDir() >> new File(rootDir)
        }

        when:
        File file = BlameFilePaths.getReportFile(build)

        then:
        file.path == new File(rootDir, 'buildtimeblamematches').path
    }
}
