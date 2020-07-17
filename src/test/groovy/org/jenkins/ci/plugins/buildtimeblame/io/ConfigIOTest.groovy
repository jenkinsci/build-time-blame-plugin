//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import com.fasterxml.jackson.databind.ObjectMapper
import hudson.model.Job
import org.jenkins.ci.plugins.buildtimeblame.analysis.RelevantStep
import spock.lang.Specification

class ConfigIOTest extends Specification {
    Job job

    void setup() {
        GroovyMock(BlameFilePaths, global: true)
        job = Mock(Job)
        _ * BlameFilePaths.getConfigFile(job) >> getTestFile()
        _ * BlameFilePaths.getLegacyConfigFile(job) >> getLegacyTestFile()
    }

    void cleanup() {
        getTestFile().delete()
        getLegacyTestFile().delete()
    }

    def 'should read steps from file'() {
        given:
        getTestFile().write('{"relevantSteps":[{"label":"Job Started","key":".*Started.*","onlyFirstMatch":true}]}')

        when:
        List<RelevantStep> relevantSteps = new ConfigIO(job).readOrDefault().relevantSteps

        then:
        relevantSteps.toListString() == [new RelevantStep(~/.*Started.*/, 'Job Started', true)].toListString()
    }

    def 'should handle missing flag when reading steps from file'() {
        given:
        getTestFile().write('{"relevantSteps":[{"key":".*Started.*","label":"Job Started"}]}')

        when:
        List<RelevantStep> relevantSteps = new ConfigIO(job).readOrDefault().relevantSteps

        then:
        relevantSteps.toListString() == [new RelevantStep(~/.*Started.*/, 'Job Started', false)].toListString()
    }

    def 'should read steps from legacy file one time if present'() {
        given:
        def legacyFileContent = '[{"key":".*Started.*","label":"Job Started","onlyFirstMatch":true}]'
        def expectedRelevantSteps = [new RelevantStep(~/.*Started.*/, 'Job Started', true)].toListString()
        getLegacyTestFile().write(legacyFileContent)

        when:
        List<RelevantStep> legacyRelevantSteps = new ConfigIO(job).readOrDefault().relevantSteps

        then:
        legacyRelevantSteps.toListString() == expectedRelevantSteps
        !getLegacyTestFile().exists()
        getTestFile().exists()

        when:
        List<RelevantStep> copiedRelevantSteps = new ConfigIO(job).readOrDefault().relevantSteps

        then:
        copiedRelevantSteps.toListString() == expectedRelevantSteps
    }

    def 'should handle missing flag when reading steps from legacy file one time if present'() {
        given:
        def fileContent = '[{"key":".*Started.*","label":"Job Started"}]'
        def expectedRelevantSteps = [new RelevantStep(~/.*Started.*/, 'Job Started', false)].toListString()

        getLegacyTestFile().write(fileContent)

        when:
        List<RelevantStep> legacyRelevantSteps = new ConfigIO(job).readOrDefault().relevantSteps

        then:
        legacyRelevantSteps.toListString() == expectedRelevantSteps
        !getLegacyTestFile().exists()
        getTestFile().exists()

        when:
        List<RelevantStep> copiedRelevantSteps = new ConfigIO(job).readOrDefault().relevantSteps

        then:
        copiedRelevantSteps.toListString() == expectedRelevantSteps
    }

    def 'should return default value if no file content'() {
        given:
        def defaultSteps = [new RelevantStep(~/.*/, 'ignored', false)]

        when:
        def config = new ConfigIO(job).readOrDefault(defaultSteps.clone() as List<RelevantStep>)

        then:
        config.relevantSteps == defaultSteps
        config.maxBuilds == null
    }

    def 'should return default value if invalid file content'() {
        given:
        def defaultSteps = [new RelevantStep(~/.*/, 'ignored', false)]
        getTestFile().write('not json')

        when:
        def config = new ConfigIO(job).readOrDefault(defaultSteps.clone() as List<RelevantStep>)

        then:
        config.relevantSteps == defaultSteps
        config.maxBuilds == null
    }

    def 'should return empty value if no file content and no default'() {
        when:
        def config = new ConfigIO(job).readOrDefault()

        then:
        config.relevantSteps == []
        config.maxBuilds == null
    }

    def 'should support parsing and reading the written config file'() {
        given:
        def config = new ReportConfiguration(maxBuilds: 8787, relevantSteps: [new RelevantStep(~/.*Finished NPM.*/, 'NPM Finished', false)])

        when:
        new ConfigIO(job).write(config)

        then:
        !getTestFile().text.empty

        when:
        def configString = getTestFile().text
        def parsedConfig = new ConfigIO(job).parse(configString)

        then:
        parsedConfig.toString() == config.toString()

        when:
        def readConfig = new ConfigIO(job).readOrDefault()

        then:
        readConfig.toString() == config.toString()
    }

    private static File getTestFile() {
        new File('temp-config.json')
    }

    private static File getLegacyTestFile() {
        new File('temp-legacy-config.json')
    }
}
