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
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.StringUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

public class JKStats implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(JKStats.class);

    private Stat stat;

    private MonitorContext context;

    private MetricWriteHelper metricWriteHelper;

    private Map<String, String> requestMap;

    private List<Metric> metrics = new ArrayList<Metric>();

    private String metricPrefix;

    private Phaser phaser;

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final String EQUAL = "=";
    private static final Pattern EQUAL_SPLIT_PATTERN = Pattern.compile(EQUAL, Pattern.LITERAL);
    private static final String DOT = ".";

    public List<Metric> getMetrics() {
        return metrics;
    }

    public JKStats(Stat stat, MonitorContext context, Map<String, String> requestMap, MetricWriteHelper metricWriteHelper, String metricPrefix, Phaser phaser) {
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
                logger.info("Not collecting mod_status stats as statsUrlPath is null for " + requestMap.get("host") + ":" + requestMap.get("port"));
                return;
            }

            String url = UrlBuilder.builder(requestMap).path(endpoint).build();

            CloseableHttpClient httpClient = this.context.getHttpClient();

            List<String> responseAsLines = HttpClientUtils.getResponseAsLines(httpClient, url);
            Multimap<String, String> jkStatusMetrics = buildMap(responseAsLines);

            Set<String> strings = jkStatusMetrics.keySet();

            Collection<String> workerNames = new ArrayList<String>();

            for (String key : strings) {
                if (key.contains("balance_workers")) {
                    Collection<String> names = jkStatusMetrics.get(key);
                    workerNames.addAll(names);
                }
            }

            logger.debug("Found workers " + workerNames);

            if (workerNames.size() > 0) {
                printJKWorkerMetrics(jkStatusMetrics, workerNames, stat);
            }
            if (metrics != null && metrics.size() > 0) {
                metricWriteHelper.transformAndPrintMetrics(metrics);
            }
        }catch(Exception e){
            logger.error("JKStats error: " + e.getMessage());
        }finally {
            logger.debug("JKStats Phaser arrived for {}", requestMap.get("host"));
            phaser.arriveAndDeregister();
        }
    }

    private void printJKWorkerMetrics(Multimap<String, String> jkStatusMetrics, Collection<String> workerNames, Stat stat) {

        for(MetricConfig metricConfig : stat.getMetricConfig()){
                 for (String workerName : workerNames) {
                    String key = getKey(workerName, metricConfig.getAttr());
                    //logger.debug("Key for worker " + workerName + " is: " + key);
                    Collection<String> values = jkStatusMetrics.get(key);
                    String metricValue = getValue(key, values);

                     /*if (key.contains("activation")) {
                        if ("ACT".equals(metricValue)) {
                            metricValue = "1";
                        } else if ("DIS".equals(metricValue)) {
                            metricValue = "2";
                        } else {
                            metricValue = "3";
                        }
                    }*/
                    if(!StringUtils.hasText(metricValue)){
                        BigDecimal bigValue = toBigDecimal(metricValue);
                        metricValue = String.valueOf(bigValue);
                    }
                    //BigDecimal bigValue = toBigDecimal(metricValue);
                    String jkMetricPath = metricPrefix + "|" + key.replace(".", "|");
                     if (metricValue != null) {
                         Map<String, String> propertiesMap = objectMapper.convertValue(metricConfig, Map.class);
                         Metric metric = new Metric(metricConfig.getAttr(), metricValue, jkMetricPath, propertiesMap);
                         metrics.add(metric);
                     }
                }

        }
    }
    protected Multimap<String, String> buildMap(List<String> lines) {
        Multimap<String, String> jkStatsMap = ArrayListMultimap.create();

        for (String line : lines) {
            String[] kv = EQUAL_SPLIT_PATTERN.split(line);
            if (kv.length == 2) {
                jkStatsMap.put(kv[0].trim(), kv[1].trim());
            }
        }
        return jkStatsMap;
    }

    private String getKey(String workerName, String workerStat) {
        StringBuilder keyBuilder = new StringBuilder("worker");
        keyBuilder.append(DOT).append(workerName).append(DOT).append(workerStat);
        return keyBuilder.toString();
    }

    private String getValue(String key, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            logger.debug("Values null for key: " + key);
            return "0";
        } else {
            return values.iterator().next();
        }
    }

    public static BigDecimal toBigDecimal(String valueStr) {
        if (StringUtils.hasText(valueStr)) {
            try {
                return new BigDecimal(valueStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert the value " + valueStr + " to BigDecimal");
            }
        }
        return null;
    }
}
