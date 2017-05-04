package com.appdynamics.monitors.apache;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.http.UrlBuilder;
import com.appdynamics.extensions.util.DeltaMetricsCalculator;
import com.appdynamics.extensions.util.GroupCounter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public class ApacheMonitoringTask implements Runnable {

    private static final Logger logger = Logger.getLogger(ApacheMonitoringTask.class);

    private MonitorConfiguration configuration;
    private Map apacheServer;
    private DeltaMetricsCalculator deltaCalculator;

    private String metricPrefix;


    private static final String COLON = ":";
    private static final Pattern COLON_SPLIT_PATTERN = Pattern.compile(COLON, Pattern.LITERAL);
    private static final String EQUAL = "=";
    private static final Pattern EQUAL_SPLIT_PATTERN = Pattern.compile(EQUAL, Pattern.LITERAL);
    private static final String DOT = ".";

    private static final String DEFAULT_METRIC_TYPE = "OBS.CUR.COL";


    public ApacheMonitoringTask(MonitorConfiguration configuration, Map apacheServer, DeltaMetricsCalculator deltaCalculator) {
        this.configuration = configuration;
        this.apacheServer = apacheServer;
        this.deltaCalculator = deltaCalculator;
        this.metricPrefix = configuration.getMetricPrefix() + "|" + apacheServer.get("displayName");
    }

    public void run() {

        Map<String, String> requestMap = buildRequestMap();
        collectAndPrintServerStats(requestMap);

        collectAndPrintJKStats(requestMap);

        collectAndPrintCustomStats(requestMap);
    }

    private void collectAndPrintCustomStats(Map<String, String> requestMap) {

        List<Map> customStatsConfigs = (List<Map>) apacheServer.get("customStats");

        if (customStatsConfigs == null || customStatsConfigs.size() <= 0) {
            logger.debug("Ignoring custom stats as no custom stat configuration is provided");
            return;
        }

        for (Map customStatsConfig : customStatsConfigs) {

            String metricPath = (String) customStatsConfig.get("metricPath");
            String metricGroup = (String) customStatsConfig.get("metricGroup");


            if (Strings.isNullOrEmpty(metricPath)) {
                logger.debug("Not collecting custom stats as custom metricPath is null for " + requestMap.get("host") + ":" + requestMap.get("port") + " and metricGroup " + metricGroup);
                continue;
            }

            String keyValueSeparator = (String) customStatsConfig.get("keyValueSeparator");
            List<String> metricsToCollect = (List<String>) customStatsConfig.get("metricsToCollect");
            List<String> deltaMetrics = (List<String>) customStatsConfig.get("deltaMetrics");

            Pattern splitPattern = null;
            if (COLON.equals(keyValueSeparator)) {
                splitPattern = COLON_SPLIT_PATTERN;
            } else if (EQUAL.equals(keyValueSeparator)) {
                splitPattern = EQUAL_SPLIT_PATTERN;
            } else {
                splitPattern = Pattern.compile(keyValueSeparator, Pattern.LITERAL);
            }

            CloseableHttpClient httpClient = configuration.getHttpClient();

            String url = UrlBuilder.builder(requestMap).path(metricPath).build();
            List<String> responseAsLines = HttpClientUtils.getResponseAsLines(httpClient, url);

            Map<String, String> customMetrics = parse(responseAsLines, splitPattern);


            for (Map.Entry<String, String> metric : customMetrics.entrySet()) {

                String metricKey = metric.getKey();


                if (metricsToCollect.contains(metricKey)) {
                    String customMetricPath = metricPrefix + "|" + metricGroup + "|" + metricKey;

                    BigDecimal bigValue = toBigDecimal(metric.getValue());

                    printMetric(customMetricPath, bigValue, DEFAULT_METRIC_TYPE);

                    if (deltaMetrics.contains(metricKey)) {
                        BigDecimal deltaMetricValue = deltaCalculator.calculateDelta(customMetricPath, bigValue);

                        printMetric(customMetricPath + " Delta", deltaMetricValue, DEFAULT_METRIC_TYPE);
                    }
                }
            }
        }
    }

    private void collectAndPrintJKStats(Map<String, String> requestMap) {
        String jkStatusPath = (String) apacheServer.get("jkStatusPath");

        if (Strings.isNullOrEmpty(jkStatusPath)) {
            logger.info("Not collecting mod_jk stats as jkStatusPath is null for " + requestMap.get("host") + ":" + requestMap.get("port"));
            return;
        }


        String url = UrlBuilder.builder(requestMap).path(jkStatusPath).build();
        CloseableHttpClient httpClient = configuration.getHttpClient();
        List<String> responseAsLines = HttpClientUtils.getResponseAsLines(httpClient, url + "?mime=prop");
        Multimap<String, String> jkStatusMetrics = buildMap(responseAsLines);

        Set<String> strings = jkStatusMetrics.keySet();

        Collection<String> workerNames = new ArrayList<String>();

        for (String key : strings) {
            if (key.contains("balance_workers")) {
                workerNames = jkStatusMetrics.get(key);
            }
        }

        logger.debug("Found workers " + workerNames);

        if (workerNames.size() > 0) {
            printJKWorkerMetrics(jkStatusMetrics, workerNames);
        }
    }

    private void printJKWorkerMetrics(Multimap<String, String> jkStatusMetrics, Collection<String> workerNames) {

        Map<String, ?> configYml = configuration.getConfigYml();
        List<Map<String, List<Map<String, String>>>> metricsFromConfig = (List<Map<String, List<Map<String, String>>>>) configYml.get("metrics");

        Map<String, List<Map<String, String>>> allMetricsFromConfig = metricsFromConfig.get(1);
        List<Map<String, String>> jkMetricNames = allMetricsFromConfig.get("jkMetrics");

        for (Map<String, String> jkMetricName : jkMetricNames) {
            String name = jkMetricName.get("name");
            String path = jkMetricName.get("path");
            String type = jkMetricName.get("type");
            String collectDelta = jkMetricName.get("collectDelta");
            String multiplier = jkMetricName.get("multiplier");

            for (String workerName : workerNames) {

                String key = getKey(workerName, name);
                Collection<String> values = jkStatusMetrics.get(key);
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
                    BigDecimal bigValue = toBigDecimal(value);
                    if (bigValue != null) {
                        String jkMetricPath = metricPrefix + "|" + path + "|" + key.replace(".", "|");

                        BigDecimal metricValueToReport = applyMultiplier(bigValue, multiplier);

                        printMetric(jkMetricPath, metricValueToReport, type);

                        if (Boolean.valueOf(collectDelta)) {

                            String deltaType = jkMetricName.get("deltaType");

                            if (Strings.isNullOrEmpty(deltaType)) {
                                deltaType = type;
                            }

                            BigDecimal deltaMetricValue = deltaCalculator.calculateDelta(jkMetricPath, metricValueToReport);

                            printMetric(jkMetricPath + " Delta", deltaMetricValue, deltaType);
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.error("Ignoring " + key + " as it can not be converted to Integer");
                }

            }
        }
    }

    private String getKey(String workerName, String workerStat) {
        StringBuilder keyBuilder = new StringBuilder("worker");
        keyBuilder.append(DOT).append(workerName).append(DOT).append(workerStat);
        return keyBuilder.toString();
    }

    private String getValue(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "0";
        } else {
            return values.iterator().next();
        }
    }

    protected void collectAndPrintServerStats(Map<String, String> requestMap) {
        String statsUrlPath = (String) apacheServer.get("statsUrlPath");

        if (Strings.isNullOrEmpty(statsUrlPath)) {
            logger.info("Not collecting mod_status stats as statsUrlPath is null for " + requestMap.get("host") + ":" + requestMap.get("port"));
            return;
        }

        String url = UrlBuilder.builder(requestMap).path(statsUrlPath).build();

        CloseableHttpClient httpClient = configuration.getHttpClient();

        List<String> responseAsLines = HttpClientUtils.getResponseAsLines(httpClient, url);
        Map<String, String> metrics = parse(responseAsLines, COLON_SPLIT_PATTERN);

        print(metrics, metricPrefix);
    }

    private void print(Map<String, String> metrics, String metricPrefix) {
        if (!metrics.isEmpty()) {
            if (!(metricPrefix.endsWith("|"))) {
                metricPrefix = metricPrefix.concat("|");
            }


            Map<String, ?> configYml = configuration.getConfigYml();
            List<Map<String, List<Map<String, String>>>> metricsFromConfig = (List<Map<String, List<Map<String, String>>>>) configYml.get("metrics");

            Map<String, List<Map<String, String>>> allMetricsFromConfig = metricsFromConfig.get(0);
            List<Map<String, String>> serverMetrics = allMetricsFromConfig.get("serverMetrics");

            printRegularMetrics(metrics, metricPrefix, serverMetrics);
            parseScoreboard(metrics, metricPrefix, serverMetrics);
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

    private Map<String, String> buildRequestMap() {
        Map<String, String> requestMap = new HashMap<String, String>();
        requestMap.put("host", (String) apacheServer.get("host"));
        requestMap.put("port", String.valueOf(apacheServer.get("port")));
        requestMap.put("use-ssl", String.valueOf(apacheServer.get("useSSL")));
        requestMap.put("username", (String) apacheServer.get("username"));
        requestMap.put("password", (String) apacheServer.get("password"));
        return requestMap;
    }

    private void printRegularMetrics(Map<String, String> valueMap, String metricPrefix, List<Map<String, String>> serverMetrics) {
        printMetric(metricPrefix + "Availability|up", BigDecimal.ONE, "OBS.AVG.COL");

        for (Map<String, String> metric : serverMetrics) {
            String name = metric.get("name");
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
            }
        }
    }

    private String getString(Long aLong) {
        if (aLong != null) {
            return aLong.toString();
        }
        return null;
    }

    private void parseScoreboard(Map<String, String> valueMap, String metricPrefix, List<Map<String, String>> serverMetrics) {
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

            for (Map<String, String> metric : serverMetrics) {
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
                }
            }
        }
    }

    public void printMetric(String metricPath, BigDecimal metricValue, String metricType) {

        if (metricValue != null) {
            configuration.getMetricWriter().printMetric(metricPath, metricValue, metricType);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Metric [" + metricType + "] metric = " + metricPath + " = " + metricValue);
        }
    }

    public BigDecimal applyMultiplier(BigDecimal metricValue, String multiplier) {

        if (Strings.isNullOrEmpty(multiplier)) {
            return metricValue;
        }

        try {
            metricValue = metricValue.multiply(new BigDecimal(multiplier));
            return metricValue;
        } catch (NumberFormatException nfe) {
            logger.error(String.format("Cannot apply multiplier {} to value {}.", multiplier, metricValue), nfe);
        }
        throw new IllegalArgumentException("Cannot convert into BigInteger " + metricValue);
    }

    public static BigDecimal toBigDecimal(String valueStr) {
        if (StringUtils.isNotBlank(valueStr)) {
            try {
                return new BigDecimal(valueStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert the value " + valueStr + " to BigDecimal");
            }
        }
        return null;
    }
}