/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.apache.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.GroupCounter;
import com.google.common.base.Strings;
import metrics.input.MetricConfig;
import metrics.input.Stat;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.regex.Pattern;

public class ServerStats implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ServerStats.class);

    private Stat stat;

    private MonitorConfiguration configuration;

    private MetricWriteHelper metricWriteHelper;

    private Map<String, String> requestMap;

    private List<Metric> metrics = new ArrayList<Metric>();

    private String metricPrefix;

    private Phaser phaser;

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final String COLON = ":";
    private static final Pattern COLON_SPLIT_PATTERN = Pattern.compile(COLON, Pattern.LITERAL);
    private static final String EQUAL = "=";

    public ServerStats(Stat stat, MonitorConfiguration configuration, Map<String, String> requestMap, MetricWriteHelper metricWriteHelper, String metricPrefix, Phaser phaser) {
        this.stat = stat;
        this.configuration = configuration;
        this.requestMap = requestMap;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix = metricPrefix;
        this.phaser = phaser;
    }

    public void run() {
        try {
            String endpoint = stat.getUrl();

            if (Strings.isNullOrEmpty(endpoint)) {
                logger.info("Not collecting mod_status stats as statsUrlPath is null for " + requestMap.get("host") + ":" + requestMap.get("port"));
                return;
            }

            String url = UrlBuilder.builder(requestMap).path(endpoint).build();

            CloseableHttpClient httpClient = this.configuration.getHttpClient();

            List<String> responseAsLines = HttpClientUtils.getResponseAsLines(httpClient, url);
            Map<String, String> responseMetrics = parse(responseAsLines, COLON_SPLIT_PATTERN);

            print(responseMetrics, metricPrefix, stat);

            if (metrics != null && metrics.size() > 0) {
                metricWriteHelper.transformAndPrintMetrics(metrics);
            }
        }catch(Exception e){
            logger.error("ServerStats error: " + e.getMessage());
        }finally {
            logger.debug("ServerStats Phaser arrived for {}", requestMap.get("host"));
            phaser.arriveAndDeregister();
        }
    }

    private Map<String, String> parse(List<String> lines, Pattern splitPattern) {
        Map<String, String> valueMap = new HashMap<String, String>();
        for (String line : lines) {
            String[] kv = splitPattern.split(line);
            if (kv.length == 2) {
                String metricName = kv[0].trim();
                String metricValue = kv[1].trim();
                valueMap.put(metricName, metricValue);
            }
        }
        logger.debug("The extracted metrics are " + valueMap);
        return valueMap;
    }

    private void print(Map<String, String> metrics, String metricPrefix, Stat stat) {
        if (!metrics.isEmpty()) {
            /*if (!(metricPrefix.endsWith("|"))) {
                metricPrefix = metricPrefix.concat("|");
            }*/

            /*Map<String, ?> configYml = this.configuration.getConfigYml();
            List<Map<String, List<Map<String, String>>>> metricsFromConfig = (List<Map<String, List<Map<String, String>>>>) configYml.get("metrics");

            Map<String, List<Map<String, String>>> allMetricsFromConfig = metricsFromConfig.get(0);
            List<Map<String, String>> serverMetrics = allMetricsFromConfig.get("serverMetrics");*/

            /*System.out.println("Size: " + stat.getStats()[0].getMetricConfig().length);
            for(MetricConfig mtr : stat.getStats()[0].getMetricConfig()){
                System.out.println("MetricConfig: " + mtr.getAlias());
            }*/
            printRegularMetrics(metrics, metricPrefix, stat);
            parseScoreboard(metrics, metricPrefix, stat.getStats()[0]);
        }
    }


    private void printRegularMetrics(Map<String, String> valueMap, String metricPrefix, Stat childStat) {
        //printMetric(metricPrefix + "Availability|up", BigDecimal.ONE, "OBS.AVG.COL");

        for (MetricConfig metricConfig : childStat.getMetricConfig()) {
            String metricValue =  valueMap.get(metricConfig.getAttr());
            if(metricValue!=null) {
                Map<String, String> propertiesMap = objectMapper.convertValue(metricConfig, Map.class);
                Metric metric = new Metric(metricConfig.getAttr(), String.valueOf(metricValue), metricPrefix + "|" + metricConfig.getAlias(), propertiesMap);
                metrics.add(metric);
            }
            /*String name = metric.get("name");
            String path = metric.get("path");
            String type = metric.get("type");
            String multiplier = metric.get("multiplier");


            String metricValue = valueMap.get(name);

            if (metricValue == null) {

                logger.debug("Ignoring metrics with null value, metric : " + path);
                continue;
            }

            BigDecimal metricValueInBigdecimal = new BigDecimal(metricValue);

            BigDecimal metricValueToReport = applyMultiplier(metricValueInBigdecimal, multiplier);


            printMetric(metricPrefix + path, metricValueToReport, type);

            String collectDelta = metric.get("collectDelta");

            if (Boolean.valueOf(collectDelta)) {

                String deltaType = metric.get("deltaType");

                if (Strings.isNullOrEmpty(deltaType)) {
                    deltaType = type;
                }

                BigDecimal deltaMetricValue = deltaCalculator.calculateDelta(metricPrefix + "|" + path, metricValueToReport);

                printMetric(metricPrefix + path + " Delta", deltaMetricValue, deltaType);
            }*/
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
            Map<String, String> curMetrics = new HashMap<String, String>();
            String waitingForConn = getString(counter.get("_"));
            curMetrics.put("Activity|Type|Waiting for Conn", waitingForConn);

            String startingUp = getString(counter.get("S"));
            curMetrics.put("Activity|Type|Starting Up", startingUp);

            String readingRequest = getString(counter.get("R"));
            curMetrics.put("Activity|Type|Reading Request", readingRequest);

            String sendingReply = getString(counter.get("W"));
            curMetrics.put("Activity|Type|Sending Reply", sendingReply);

            String keepAlive = getString(counter.get("K"));
            curMetrics.put("Activity|Type|Keep Alive", keepAlive);

            String dnsLookup = getString(counter.get("D"));
            curMetrics.put("Activity|Type|DNS Lookup", dnsLookup);

            String closingConnections = getString(counter.get("C"));
            curMetrics.put("Activity|Type|Closing Connection", closingConnections);

            String logging = getString(counter.get("L"));
            curMetrics.put("Activity|Type|Logging", logging);

            String gracefullyFinishing = getString(counter.get("G"));
            curMetrics.put("Activity|Type|Gracefully Finishing", gracefullyFinishing);

            String cleaningUp = getString(counter.get("I"));
            curMetrics.put("Activity|Type|Cleaning Up", cleaningUp);

            for (MetricConfig metricConfig : childStat.getMetricConfig()) {
                String metricValue =  curMetrics.get(metricConfig.getAlias());
                if(metricValue!=null) {
                    Map<String, String> propertiesMap = objectMapper.convertValue(metricConfig, Map.class);
                    Metric metric = new Metric(metricConfig.getAttr(), String.valueOf(metricValue), metricPrefix + "|" + metricConfig.getAlias(), propertiesMap);
                    metrics.add(metric);
                }

            /*for (Map<String, String> metric : serverMetrics) {
                String path = metric.get("path");
                String type = metric.get("type");
                String multiplier = metric.get("multiplier");

                if (curMetrics.containsKey(path)) {

                    String metricValue = curMetrics.get(path);

                    if (metricValue == null) {

                        logger.debug("Ignoring metrics with null value, metric : " + path);
                        continue;
                    }

                    BigDecimal metricValueInBigdecimal = new BigDecimal(metricValue);

                    BigDecimal metricValueToReport = applyMultiplier(metricValueInBigdecimal, multiplier);

                    printMetric(metricPrefix + path, metricValueToReport, type);

                    String collectDelta = metric.get("collectDelta");

                    if (Boolean.valueOf(collectDelta)) {

                        String deltaType = metric.get("deltaType");

                        if (Strings.isNullOrEmpty(deltaType)) {
                            deltaType = type;
                        }

                        BigDecimal deltaMetricValue = deltaCalculator.calculateDelta(metricPrefix + "|" + path, metricValueToReport);

                        printMetric(metricPrefix + path + " Delta", deltaMetricValue, deltaType);
                    }
                }*/
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
