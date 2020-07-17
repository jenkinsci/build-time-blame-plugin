//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.action;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

@Extension
public class BlameActionFactory extends TransientActionFactory<Job> {
    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        return Collections.singletonList(new BlameAction(job));
    }
}
