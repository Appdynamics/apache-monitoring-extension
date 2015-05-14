package com.appdynamics.monitors.apache;

import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.io.Lines;
import com.appdynamics.extensions.util.GroupCounter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/21/14
 * Time: 7:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApacheStatusMonitor extends AManagedMonitor {
    public static final Logger logger = LoggerFactory.getLogger(ApacheStatusMonitor.class);
    private static final Pattern SPLIT_PATTERN = Pattern.compile(":", Pattern.LITERAL);
    private static final Pattern EQUAL_SPLIT_PATTERN = Pattern.compile("=", Pattern.LITERAL);
    private static final String DOT = ".";

    //default values if not specified
    private static final Map<String, String> DEFAULT_ARGS = new HashMap<String, String>() {{
        put("custom-url-path", "server-status?auto");
        put("metric-prefix", "Custom Metrics|WebServer|Apache|Status");
    }};

    public ApacheStatusMonitor() {
        String version = getClass().getPackage().getImplementationTitle();
        String msg = String.format("Using Monitor Version [%s]", version);
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> argsMap, TaskExecutionContext executionContext) throws TaskExecutionException {
        logger.debug("The args map before filling the default is {}", argsMap);
        argsMap = ArgumentsValidator.validateArguments(argsMap, DEFAULT_ARGS);
        logger.debug("The args map after filling the default is {}", argsMap);
        SimpleHttpClient httpClient = buildHttpClient(argsMap);
        try {
            getServerStats(argsMap, httpClient);

            String jkStatusPath = argsMap.get("jk-status-path");
            if (!Strings.isNullOrEmpty(jkStatusPath)) {
                //If we have jk status path then get the jk stats in properties format
                getJKStats(argsMap, httpClient, jkStatusPath + "?mime=prop");
            }
        } finally {
            httpClient.close();
        }
        return new TaskOutput("Apache Monitor Completed");

    }

    private void getJKStats(Map<String, String> argsMap, SimpleHttpClient httpClient, String jkStatusPath) throws TaskExecutionException {
        String url = UrlBuilder.builder(argsMap).path(jkStatusPath).build();
        try {
            Lines lines = httpClient.target(url).get().lines();
            Multimap<String, String> jkStatsMap = buildMap(lines);

            String jkWorkerStatsStr = argsMap.get("jk-worker-stats");
            String[] jkWorkerStats = jkWorkerStatsStr.split(",");
            String jkMetricPrefix = argsMap.get("metric-prefix") + "| JK Status";
            printJKStats(jkStatsMap, jkMetricPrefix, jkWorkerStats);


        } catch (Exception e) {
            logger.error("Exception while getting the apache JK stats from the URL " + url, e);
            throw new TaskExecutionException("Exception while getting the apache JK stats from the URL " + url, e);
        }
    }

    private void printJKStats(Multimap<String, String> jkStatsMap, String jkMetricPrefix, String[] jkWorkerStats) {

        Set<String> strings = jkStatsMap.keySet();
        for (String key : strings) {
            if (key.contains("balance_workers")) {
                Collection<String> workers = jkStatsMap.get(key);
                for (String workerName : workers) {
                    printJKWorkerMetrics(workerName, jkWorkerStats, jkStatsMap, jkMetricPrefix);
                }
            }
        }
    }

    private void printJKWorkerMetrics(String workerName, String[] jkWorkerStats, Multimap<String, String> jkStatsMap, String jkMetricPrefix) {

        for (String workerStat : jkWorkerStats) {
            String key = getKey(workerName, workerStat);
            Collection<String> values = jkStatsMap.get(key);
            String value = getValue(values);
            if (key.contains("activation")) {
                if ("ACT".equals(value)) {
                    value = "1";
                } else if ("DIS".equals(value)) {
                    value = "2";
                } else {
                    value = "3";
                }
            }
            try {
                //Integer.parseInt(value);
                BigDecimal bigValue = toBigDecimal(value);
                printCollectiveObservedCurrent(jkMetricPrefix + "|" + key.replace(".", "|"), bigValue.toString());
            } catch (NumberFormatException e) {
                logger.error("Ignoring " + key + " as it can not be converted to Integer");
            }
        }
    }

    private String getValue(Collection<String> values) {
        if (values == null) {
            return "0";
        } else {
            return values.iterator().next();
        }
    }

    private String getKey(String workerName, String workerStat) {
        StringBuilder keyBuilder = new StringBuilder("worker");
        keyBuilder.append(DOT).append(workerName).append(DOT).append(workerStat);
        return keyBuilder.toString();
    }

    private Multimap<String, String> buildMap(Lines lines) {
        Multimap<String, String> jkStatsMap = ArrayListMultimap.create();

        for (String line : lines) {
            String[] kv = EQUAL_SPLIT_PATTERN.split(line);
            if (kv.length == 2) {
                jkStatsMap.put(kv[0].trim(), kv[1].trim());
            }
        }

        return jkStatsMap;
    }

    private void getServerStats(Map<String, String> argsMap, SimpleHttpClient httpClient) throws TaskExecutionException {
        String url = UrlBuilder.builder(argsMap).path(argsMap.get("custom-url-path")).build();
        try {
            Lines lines = httpClient.target(url).get().lines();
            parse(lines, argsMap);
        } catch (Exception e) {
            logger.error("Exception while getting the apache status from the URL " + url, e);
            throw new TaskExecutionException("Exception while getting the apache status from the URL " + url, e);
        }
    }

    protected SimpleHttpClient buildHttpClient(Map<String, String> argsMap) {
        return SimpleHttpClient.builder(argsMap).build();
    }

    private void parse(Lines lines, Map<String, String> argsMap) {
        Map<String, String> valueMap = new HashMap<String, String>();
        for (String line : lines) {
            String[] kv = SPLIT_PATTERN.split(line);
            if (kv.length == 2) {
                valueMap.put(kv[0].trim(), kv[1].trim());
            }
        }
        logger.debug("The extracted metrics are {}", valueMap);
        if (!valueMap.isEmpty()) {
            String metricPrefix = argsMap.get("metric-prefix") + "|";
            printRegularMetrics(valueMap, metricPrefix);
            parseScoreboard(valueMap, metricPrefix);
        }
    }

    /**
     * Counts the occurrence of each character in the scoreboard and report it.
     *
     * @param valueMap
     * @param metricPrefix
     */
    private void parseScoreboard(Map<String, String> valueMap, String metricPrefix) {
        String scoreboard = valueMap.get("Scoreboard");
        if (scoreboard != null && !scoreboard.isEmpty()) {
            //Count the occurrences of each character
            GroupCounter<String> counter = new GroupCounter<String>();
            char[] chars = scoreboard.toCharArray();
            for (char aChar : chars) {
                counter.increment(Character.toString(aChar));
            }
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Waiting for Conn", getString(counter.get("_")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Starting Up", getString(counter.get("S")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Reading Request", getString(counter.get("R")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Sending Reply", getString(counter.get("W")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Keep Alive", getString(counter.get("K")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|DNS Lookup", getString(counter.get("D")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Closing Connection", getString(counter.get("S")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Logging", getString(counter.get("L")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Gracefully Finishing", getString(counter.get("G")));
            printCollectiveObservedCurrent(metricPrefix + "Activity|Type|Cleaning Up", getString(counter.get("I")));
        }
    }

    private String getString(Long aLong) {
        if (aLong != null) {
            return aLong.toString();
        }
        return null;
    }

    private void printRegularMetrics(Map<String, String> valueMap, String metricPrefix) {
        printCollectiveObservedAverage(metricPrefix + "Availability|up", String.valueOf(1));
        printCollectiveObservedCurrent(metricPrefix + "Availability|Server Uptime (sec)", valueMap.get("Uptime"));
        //Resource Utilization
        printCollectiveObservedAverage(metricPrefix + "Resource Utilization|CPU|Load", round(valueMap.get("CPULoad")));
        printCollectiveObservedAverage(metricPrefix + "Resource Utilization|Processes|Busy Workers", valueMap.get("BusyWorkers"));
        printCollectiveObservedAverage(metricPrefix + "Resource Utilization|Processes|Idle Workers", valueMap.get("IdleWorkers"));
        printCollectiveObservedAverage(metricPrefix + "Resource Utilization|ConnsAsyncClosing", valueMap.get("ConnsAsyncClosing"));
        printCollectiveObservedAverage(metricPrefix + "Resource Utilization|ConnsAsyncKeepAlive", valueMap.get("ConnsAsyncKeepAlive"));
        printCollectiveObservedAverage(metricPrefix + "Resource Utilization|ConnsAsyncWriting", valueMap.get("ConnsAsyncWriting"));
        printCollectiveObservedAverage(metricPrefix + "Resource Utilization|Total Connections", valueMap.get("ConnsTotal"));
        // Activity
        printCollectiveObservedCurrent(metricPrefix + "Activity|Total Accesses", valueMap.get("Total Accesses"));
        printCollectiveObservedCurrent(metricPrefix + "Activity|Total Traffic", round(valueMap.get("Total kBytes")));
        printCollectiveObservedAverage(metricPrefix + "Activity|Requests/min", convertSecToMin(valueMap.get("ReqPerSec")));
        printCollectiveObservedAverage(metricPrefix + "Activity|Bytes/min", convertSecToMin(valueMap.get("BytesPerSec")));
        printCollectiveObservedAverage(metricPrefix + "Activity|Bytes/req", round(valueMap.get("BytesPerReq")));
    }

    private String round(String s) {
        if (s != null && !s.trim().isEmpty()) {
            try {
                return new BigDecimal(s.trim()).setScale(0, RoundingMode.HALF_UP).toString();
            } catch (Exception e) {
                logger.error("Error while rounding the value {}", s);
            }
        }
        return null;
    }

    protected void printCollectiveObservedCurrent(String metricName, String metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    protected void printCollectiveObservedAverage(String metricName, String metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    public void printMetric(String metricPath, String metricValue, String aggregation, String timeRollup, String cluster) {

        MetricWriter metricWriter = getMetricWriter(metricPath,
                aggregation,
                timeRollup,
                cluster
        );
        if (metricValue != null) {
            metricWriter.printMetric(metricValue.toString());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Metric [" + aggregation + "/" + timeRollup + "/" + cluster
                    + "] metric = " + metricPath + " = " + metricValue);
        }
    }

    public String convertSecToMin(String valueStr) {
        BigDecimal bd = toBigDecimal(valueStr);
        if (bd != null) {
            return bd.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).toString();
        }
        return null;
    }

    public String convertToPercent(String valueStr) {
        BigDecimal bd = toBigDecimal(valueStr);
        if (bd != null) {
            return bd.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).toString();
        }
        return null;
    }

    private static BigDecimal toBigDecimal(String valueStr) {
        if (valueStr != null && !valueStr.trim().isEmpty()) {
            try {
                return new BigDecimal(valueStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert the value {} to string", valueStr);
            }
        }
        return null;
    }
}
