package com.appdynamics.monitors.apache;

import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.io.Lines;
import com.appdynamics.extensions.util.GroupCounter;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
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
        String url = UrlBuilder.builder(argsMap).path(argsMap.get("custom-url-path")).build();
        try {
            Lines lines = httpClient.target(url).get().lines();
            parse(lines, argsMap);
            return new TaskOutput("Apache Monitor Completed");
        } catch (Exception e) {
            logger.error("Exception while getting the apache status from the URL " + url, e);
            throw new TaskExecutionException(e);
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
