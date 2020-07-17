//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io


import hudson.model.Job
import hudson.model.PersistenceRoot
import hudson.model.Run

class BlameFilePaths {
    static File getConfigFile(Job job) {
        getFile(job, 'build-time-blame-config.json')
    }

    static File getLegacyConfigFile(Job job) {
        getFile(job, 'buildtimeblameconfig')
    }

    static File getReportFile(Run run) {
        getFile(run, 'build-time-blame-matches.json')
    }

    private static File getFile(PersistenceRoot persistenceRoot, String fileName) {
        new File(persistenceRoot.rootDir, fileName)
    }
}
