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
import com.appdynamics.extensions.util.GroupCounter;
import com.google.common.base.Strings;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

public class ServerStats implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ServerStats.class);

    private Stat stat;

    private MonitorContext context;

    private MetricWriteHelper metricWriteHelper;

    private Map<String, String> requestMap;

    private List<Metric> metrics = new ArrayList<Metric>();

    private String metricPrefix;

    private Phaser phaser;

    private static ObjectMapper objectMapper = new ObjectMapper();

    private MetricsUtil metricsUtil = new MetricsUtil();

    private static final String COLON = ":";
    private static final Pattern COLON_SPLIT_PATTERN = Pattern.compile(COLON, Pattern.LITERAL);

    public List<Metric> getMetrics() {
        return metrics;
    }

    public ServerStats(Stat stat, MonitorContext context, Map<String, String> requestMap, MetricWriteHelper metricWriteHelper, String metricPrefix, Phaser phaser) {
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
            String endpoint = stat.getUrl();

            if (Strings.isNullOrEmpty(endpoint)) {
                logger.info("Not collecting server stats as statsUrlPath is null for " + requestMap.get("host") + ":" + requestMap.get("port"));
                return;
            }

            Map<String, String> responseMetrics = metricsUtil.fetchResponse(requestMap, endpoint,this.context.getHttpClient(), COLON_SPLIT_PATTERN);

            print(responseMetrics, metricPrefix, stat);

            metrics.add(new Metric("HeartBeat", String.valueOf(BigInteger.ONE), metricPrefix + "|HeartBeat", "AVG", "AVG", "IND"));
            if (metrics != null && metrics.size() > 0) {
                metricWriteHelper.transformAndPrintMetrics(metrics);
            }
        }catch(Exception e){
            logger.error("ServerStats error: " + e.getMessage());
            metricWriteHelper.printMetric(metricPrefix + "|HeartBeat", BigDecimal.ZERO, "AVG.AVG.IND");
        }finally {
            logger.debug("ServerStats Phaser arrived for {}", requestMap.get("host"));
            phaser.arriveAndDeregister();
        }
    }


    private void print(Map<String, String> metrics, String metricPrefix, Stat stat) {
        if (!metrics.isEmpty()) {
            printRegularMetrics(metrics, metricPrefix, stat);
            parseScoreboard(metrics, metricPrefix, stat.getStats()[0]);
        }
    }


    private void printRegularMetrics(Map<String, String> valueMap, String metricPrefix, Stat childStat) {

        for (MetricConfig metricConfig : childStat.getMetricConfig()) {
            String metricValue =  valueMap.get(metricConfig.getAttr());
            if(metricValue!=null) {
                Map<String, String> propertiesMap = objectMapper.convertValue(metricConfig, Map.class);
                Metric metric = new Metric(metricConfig.getAttr(), String.valueOf(metricValue), metricPrefix + "|" + metricConfig.getAlias(), propertiesMap);
                metrics.add(metric);
            }
        }
    }

    private void parseScoreboard(Map<String, String> valueMap, String metricPrefix, Stat childStat) {
        String scoreboard = valueMap.get("Scoreboard");
        if (!Strings.isNullOrEmpty(scoreboard)) {
            //Count the occurrences of each character
            GroupCounter<String> counter = new GroupCounter<String>();
            char[] chars = scoreboard.toCharArray();
            for (char aChar : chars) {
                counter.increment(Character.toString(aChar));
            }

            for (MetricConfig metricConfig : childStat.getMetricConfig()) {

                String metricValue =  getString(counter.get(metricConfig.getCharacter()));
                if(metricValue!=null) {
                    Map<String, String> propertiesMap = objectMapper.convertValue(metricConfig, Map.class);
                    Metric metric = new Metric(metricConfig.getAttr(), String.valueOf(metricValue), metricPrefix + "|" + metricConfig.getAlias(), propertiesMap);
                    metrics.add(metric);
                }
            }
        }
    }

    private String getString(Long aLong) {
        if (aLong != null) {
            return aLong.toString();
        }
        return null;
    }

}
