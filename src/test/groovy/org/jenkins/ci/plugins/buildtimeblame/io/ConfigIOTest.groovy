//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import org.jenkins.ci.plugins.buildtimeblame.analysis.RelevantStep
import hudson.model.AbstractProject
import net.sf.json.JSONObject
import spock.lang.Specification

class ConfigIOTest extends Specification {
    AbstractProject project

    void setup() {
        GroovyMock(BlameFilePaths, global: true)
        project = Mock(AbstractProject)
        _ * BlameFilePaths.getConfigFile(project) >> getTestFile()
    }

    void cleanup() {
        getTestFile().delete()
    }

    def 'should serialize steps to file'() {
        given:
        def relevantSteps = [new RelevantStep(~/.*any.*/, 'label value', false)]

        when:
        new ConfigIO(project).write(relevantSteps)

        then:
        getTestFile().text == '[{"key":".*any.*","label":"label value","onlyFirstMatch":false}]'
    }

    def 'should read steps from file'() {
        given:
        getTestFile().write('[{"key":".*Started.*","label":"Job Started","onlyFirstMatch":true}]')

        when:
        List<RelevantStep> relevantSteps = new ConfigIO(project).readOrDefault()

        then:
        relevantSteps.toListString() == [new RelevantStep(~/.*Started.*/, 'Job Started', true)].toListString()
    }

    def 'should handle missing flag when reading steps from file'() {
        given:
        getTestFile().write('[{"key":".*Started.*","label":"Job Started"}]')

        when:
        List<RelevantStep> relevantSteps = new ConfigIO(project).readOrDefault()

        then:
        relevantSteps.toListString() == [new RelevantStep(~/.*Started.*/, 'Job Started', false)].toListString()
    }

    def 'should read steps from List of Json Objects with default value for onlyFirstMatch'() {
        given:
        def input = [
                JSONObject.fromObject('{"key":".*Started NPM.*","label":"NPM Started"}'),
                JSONObject.fromObject('{"key":".*Started Bower.*","label":"Bower Started"}'),
        ]

        when:
        List<RelevantStep> relevantSteps = ConfigIO.readValue(input)

        then:
        relevantSteps.toListString() == [
                new RelevantStep(~/.*Started NPM.*/, 'NPM Started', false),
                new RelevantStep(~/.*Started Bower.*/, 'Bower Started', false),
        ].toListString()
    }

    def 'should read steps from List of Json Objects with string value for onlyFirstMatch'() {
        given:
        def input = [
                JSONObject.fromObject('{"key":".*Started NPM.*","label":"NPM Started","onlyFirstMatch":"true"}'),
                JSONObject.fromObject('{"key":".*Started Bower.*","label":"Bower Started","onlyFirstMatch":"false"}'),
        ]

        when:
        List<RelevantStep> relevantSteps = ConfigIO.readValue(input)

        then:
        relevantSteps.toListString() == [
                new RelevantStep(~/.*Started NPM.*/, 'NPM Started', true),
                new RelevantStep(~/.*Started Bower.*/, 'Bower Started', false),
        ].toListString()
    }

    def 'should read steps from List of Json Objects with boolean value for onlyFirstMatch'() {
        given:
        def input = [
                JSONObject.fromObject('{"key":".*Started NPM.*","label":"NPM Started","onlyFirstMatch":false}'),
                JSONObject.fromObject('{"key":".*Started Bower.*","label":"Bower Started","onlyFirstMatch":true}'),
        ]

        when:
        List<RelevantStep> relevantSteps = ConfigIO.readValue(input)

        then:
        relevantSteps.toListString() == [
                new RelevantStep(~/.*Started NPM.*/, 'NPM Started', false),
                new RelevantStep(~/.*Started Bower.*/, 'Bower Started', true),
        ].toListString()
    }

    def 'should use same format for read and write'() {
        given:
        def expected = [new RelevantStep(~/.*Finished NPM.*/, 'NPM Finished', false)]

        when:
        new ConfigIO(project).write(expected.clone() as List<RelevantStep>)
        def relevantSteps = new ConfigIO(project).readOrDefault()

        then:
        relevantSteps.toListString() == expected.toListString()
    }

    def 'should return default value if no file content'() {
        given:
        def defaultValue = [new RelevantStep(~/.*/, 'ignored', false)]

        when:
        def relevantSteps = new ConfigIO(project).readOrDefault(defaultValue.clone() as List<RelevantStep>)

        then:
        relevantSteps == defaultValue
    }

    def 'should return default value if invalid file content'() {
        given:
        def defaultValue = [new RelevantStep(~/.*/, 'ignored', false)]
        getTestFile().write('not json')

        when:
        def relevantSteps = new ConfigIO(project).readOrDefault(defaultValue.clone() as List<RelevantStep>)

        then:
        relevantSteps == defaultValue
    }

    def 'should return empty value if no file content and no default'() {
        when:
        def relevantSteps = new ConfigIO(project).readOrDefault()

        then:
        relevantSteps == []
    }

    private static File getTestFile() {
        new File('tempconfig.txt')
    }
}
