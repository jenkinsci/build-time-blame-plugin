//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.action;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.Collection;
import java.util.Collections;

@Extension
public class BlameActionFactory extends TransientProjectActionFactory {
    @Override
    public Collection<? extends Action> createFor(AbstractProject target) {
        return Collections.singletonList(new BlameAction(target));
    }
}
