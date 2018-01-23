/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.registry.notification;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.registry.notification.events.EventType;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookCause;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Provides environment variables to builds triggered by the plugin.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
@Extension
@Restricted(NoExternalUse.class)
public class EnvContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        WebHookCause cause = (WebHookCause)r.getCause(WebHookCause.class);
        if (cause != null) {
            Set<ParameterValue> parameters = cause.getPushNotification().getRunParameters();
            for (ParameterValue parameter : parameters) {
                parameter.buildEnvironment(r, envs);
            }
            final Job parent = r.getParent();
            if (parent instanceof ParameterizedJobMixIn.ParameterizedJob) {
                final DockerHubTrigger trigger = DockerHubTrigger.getTrigger((ParameterizedJobMixIn.ParameterizedJob)parent);
                if (trigger != null) {
                    final List<EventType> eventTypes = trigger.getEventTypes();
                    final String dtrJsonType = cause.getPushNotification().getDtrEventJSONTypeEventJSONType();
                    for (EventType type : eventTypes) {
                        if (type.accepts(dtrJsonType)) {
                            type.buildEnvironment(envs, cause.getPushNotification());
                        }
                    }
                }
            }

        }
    }
}
