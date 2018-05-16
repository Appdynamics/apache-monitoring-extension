/*
 *
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.apache;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.apache.input.Stat;
import com.appdynamics.extensions.util.AssertUtils;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom, Satish M, Akshay S
 * Date: 4/21/14
 * Time: 7:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApacheMonitor extends ABaseMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|Apache";

    private static final Logger logger = LoggerFactory.getLogger(ApacheMonitor.class);


    @Override
    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "Apache Monitor";
    }


    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {

        List<Map> apacheServers = (List<Map>) this.getContextConfiguration().getConfigYml().get("servers");

        AssertUtils.assertNotNull(this.getContextConfiguration().getMetricsXml(), "Metrics xml not available");
        AssertUtils.assertNotNull(apacheServers, "The 'servers' section in config.yml is not initialised");

        for (Map apacheServer : apacheServers) {

            ApacheMonitorTask task = new ApacheMonitorTask(serviceProvider, this.getContextConfiguration(), apacheServer);
            AssertUtils.assertNotNull(apacheServer.get("displayName"), "The displayName can not be null");
            serviceProvider.submit((String) apacheServer.get("displayName"), task);
        }
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        this.getContextConfiguration().setMetricXml(args.get("metric-file"), Stat.Stats.class);

    }

    @Override
    protected int getTaskCount() {
        List<Map<String, String>> servers = (List<Map<String, String>>) getContextConfiguration().getConfigYml().get("servers");
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers.size();
    }
}
