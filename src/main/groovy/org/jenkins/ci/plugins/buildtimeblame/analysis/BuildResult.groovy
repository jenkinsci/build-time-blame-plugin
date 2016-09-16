//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.analysis

import hudson.model.Run

class BuildResult {
    List<ConsoleLogMatch> consoleLogMatches
    Run build
}
