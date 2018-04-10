/*
 *
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.apache;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.apache.input.Stat;
import com.appdynamics.extensions.apache.metrics.CustomStats;
import com.appdynamics.extensions.apache.metrics.JKStats;
import com.appdynamics.extensions.apache.metrics.ServerStats;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.StringUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Phaser;

/**
 * @author Satish Muddam
 */
public class ApacheMonitoringTask implements AMonitorTaskRunnable {

    private static final Logger logger = Logger.getLogger(ApacheMonitoringTask.class);

    private MonitorConfiguration configuration;

    private Map apacheServer;

    private MetricWriteHelper metricWriter;

    private String metricPrefix;

    private String displayName;

    public ApacheMonitoringTask(TasksExecutionServiceProvider serviceProvider, Map apacheServer) {
        this.configuration = serviceProvider.getMonitorConfiguration();
        this.apacheServer = apacheServer;
        this.metricPrefix = configuration.getMetricPrefix() + "|" + apacheServer.get("displayName");
        this.metricWriter = serviceProvider.getMetricWriteHelper();
        this.displayName = (String)apacheServer.get("displayName");
    }

    public void run() {
        try {
            Phaser phaser = new Phaser();
            Stat.Stats metricConfig = (Stat.Stats) configuration.getMetricsXmlConfiguration();
            Map<String, String> requestMap = buildRequestMap();

            for(Stat stat: metricConfig.getStats()) {
                if(StringUtils.hasText(stat.getName()) && stat.getName().equalsIgnoreCase("serverMetrics")) {
                    phaser.register();
                    ServerStats serverMetricTask = new ServerStats(stat, configuration, requestMap, metricWriter, metricPrefix, phaser);
                    configuration.getExecutorService().execute("MetricCollectorTask", serverMetricTask);
                    logger.debug("Registering MetricCollectorTask phaser for " + displayName);
                }else if(StringUtils.hasText(stat.getName()) && stat.getName().equalsIgnoreCase("jkMetrics")){
                    phaser.register();
                    JKStats jkMetricTask = new JKStats(stat, configuration, requestMap, metricWriter, metricPrefix, phaser);
                    configuration.getExecutorService().execute("MetricCollectorTask", jkMetricTask);
                    logger.debug("Registering MetricCollectorTask phaser for " + displayName);
                }else{
                    phaser.register();
                    CustomStats customMetricTask = new CustomStats(stat, configuration, requestMap, metricWriter, metricPrefix, phaser);
                    configuration.getExecutorService().execute("MetricCollectorTask", customMetricTask);
                    logger.debug("Registering MetricCollectorTask phaser for " + displayName);
                }
            }
            //Wait for all tasks to finish
            phaser.arriveAndAwaitAdvance();
            logger.info("Completed the Apache Monitoring task");

        }catch(Exception e) {
            logger.error("Unexpected error while running the Apache Monitor", e);
        }
    }

    private Map<String, String> buildRequestMap() {
        Map<String, String> requestMap = new HashMap<String, String>();
        requestMap.put("host", (String) apacheServer.get("host"));
        requestMap.put("port", String.valueOf(apacheServer.get("port")));
        requestMap.put("use-ssl", String.valueOf(apacheServer.get("useSSL")));
        requestMap.put("username", (String) apacheServer.get("username"));
        requestMap.put("password", (String) apacheServer.get("password"));
        return requestMap;
    }

    public void onTaskComplete() {
        logger.info("All tasks for server {} finished");
    }
}