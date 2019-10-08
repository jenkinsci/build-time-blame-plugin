//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io


import hudson.model.Job
import hudson.model.PersistenceRoot
import hudson.model.Run

class BlameFilePaths {
    static File getConfigFile(Job job) {
        getFile(job, 'buildtimeblameconfig')
    }

    static File getReportFile(Run run) {
        getFile(run, 'buildtimeblamematches')
    }

    private static File getFile(PersistenceRoot persistenceRoot, String fileName) {
        new File(persistenceRoot.rootDir, fileName)
    }
}
