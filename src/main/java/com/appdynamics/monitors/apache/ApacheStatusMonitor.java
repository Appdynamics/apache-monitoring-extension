/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */


package com.appdynamics.monitors.apache;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.AssertUtils;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import metrics.input.Stat;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom, Satish M
 * Date: 4/21/14
 * Time: 7:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApacheStatusMonitor extends ABaseMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|WebServer|Apache|Status|";

    private static final Logger logger = Logger.getLogger(ApacheStatusMonitor.class);


    @Override
    protected String getDefaultMetricPrefix() {
        return METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return "Apache Status Monitor";
    }


    @Override
    protected void initializeMoreStuff(Map<String, String> args, MonitorConfiguration conf) {
        conf.setMetricsXml(args.get("metric-file"), Stat.Stats.class);

    }


    @Override
    protected void doRun(TasksExecutionServiceProvider serviceProvider) {

        List<Map> apacheServers = (List<Map>) this.configuration.getConfigYml().get("servers");

        AssertUtils.assertNotNull(this.configuration.getMetricsXmlConfiguration(), "Metrics xml not available");
        AssertUtils.assertNotNull(apacheServers, "The 'servers' section in config.yml is not initialised");

        for (Map apacheServer : apacheServers) {

            ApacheMonitoringTask task = new ApacheMonitoringTask(serviceProvider, apacheServer);
            AssertUtils.assertNotNull(apacheServer.get("displayName"), "The displayName can not be null");
            serviceProvider.submit((String) apacheServer.get("displayName"), task);
        }
    }

    @Override
    protected int getTaskCount() {
        List<Map<String, String>> servers = (List<Map<String, String>>) configuration.getConfigYml().get("servers");
        AssertUtils.assertNotNull(servers, "The 'servers' section in config.yml is not initialised");
        return servers.size();
    }


    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        logger.getRootLogger().addAppender(ca);

        final ApacheStatusMonitor monitor = new ApacheStatusMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "/Users/akshay.srivastava/AppDynamics/extensions/apache-monitoring-extension/src/main/resources/conf/config.yml");
        taskArgs.put("metric-file", "/Users/akshay.srivastava/AppDynamics/extensions/apache-monitoring-extension/src/main/resources/conf/metrics.xml");

        //monitor.execute(taskArgs, null);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 30, TimeUnit.SECONDS);
    }
}
