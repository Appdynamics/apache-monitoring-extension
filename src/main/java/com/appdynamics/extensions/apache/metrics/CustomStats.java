/*
 *
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.apache.metrics;

import com.appdynamics.extensions.apache.input.MetricConfig;
import com.appdynamics.extensions.apache.input.Stat;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

public class CustomStats implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(CustomStats.class);

    private Stat stat;

    private MonitorContext context;

    private MetricWriteHelper metricWriteHelper;

    private Map<String, String> requestMap;

    private List<Metric> metrics = new ArrayList<Metric>();

    private String metricPrefix;

    private Phaser phaser;

    private static ObjectMapper objectMapper = new ObjectMapper();

    private MetricsUtil metricsUtil = new MetricsUtil();

    public List<Metric> getMetrics() {
        return metrics;
    }

    private static final String COLON = ":";
    private static final Pattern COLON_SPLIT_PATTERN = Pattern.compile(COLON, Pattern.LITERAL);
    private static final String EQUAL = "=";
    private static final Pattern EQUAL_SPLIT_PATTERN = Pattern.compile(EQUAL, Pattern.LITERAL);

    public CustomStats(Stat stat,  MonitorContext context, Map<String, String> requestMap, MetricWriteHelper metricWriteHelper, String metricPrefix, Phaser phaser) {
        this.stat = stat;
        this.context = context;
        this.requestMap = requestMap;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = metricPrefix;
        this.phaser = phaser;
    }

    public void run() {

        try {
            phaser.register();
            for(Stat childStat: stat.getStats()) {
                String endpoint = childStat.getUrl();

                if (Strings.isNullOrEmpty(endpoint)) {
                    logger.info("Not collecting custom stats as statsUrlPath is null for " + requestMap.get("host") + ":" + requestMap.get("port"));
                    return;
                }
                Pattern splitPattern;
                if (COLON.equals(childStat.getKeyValueSeparator())) {
                    splitPattern = COLON_SPLIT_PATTERN;
                } else if (EQUAL.equals(childStat.getKeyValueSeparator())) {
                    splitPattern = EQUAL_SPLIT_PATTERN;
                } else {
                    splitPattern = Pattern.compile(childStat.getKeyValueSeparator(), Pattern.LITERAL);
                }

                Map<String, String> responseMetrics = metricsUtil.fetchResponse(requestMap, endpoint,this.context.getHttpClient(), splitPattern);
                printCustomStats(responseMetrics, childStat);
            }
            if (metrics != null && metrics.size() > 0) {
                metricWriteHelper.transformAndPrintMetrics(metrics);
            }
        }catch(Exception e){
            logger.error("CustomStats error: " + e.getMessage());
        }finally {
            logger.debug("CustomStats Phaser arrived for {}", requestMap.get("host"));
            phaser.arriveAndDeregister();
        }
    }

    private void printCustomStats(Map<String, String> valueMap, Stat childStat) {

         if (childStat.getMetricConfig() == null || childStat.getMetricConfig().length <= 0) {
            logger.debug("Ignoring custom stats as no custom stat configuration is provided");
            return;
         }

        for (MetricConfig metricConfig : childStat.getMetricConfig()) {
            String metricValue = valueMap.get(metricConfig.getAttr());
            if (metricValue != null) {
                Map<String, String> propertiesMap = objectMapper.convertValue(metricConfig, Map.class);
                Metric metric = new Metric(metricConfig.getAttr(), String.valueOf(metricValue), metricPrefix + "|" + metricConfig.getAlias(), propertiesMap);
                metrics.add(metric);
            }
        }

    }
}
