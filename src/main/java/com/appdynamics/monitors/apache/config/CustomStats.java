package com.appdynamics.monitors.apache.config;


import java.util.List;

public class CustomStats {

    private String metricGroup;
    private String metricPath;
    private String keyValueSeparator;
    private List<String> metricsToCollect;
    private List<String> deltaStats;

    public String getMetricGroup() {
        return metricGroup;
    }

    public void setMetricGroup(String metricGroup) {
        this.metricGroup = metricGroup;
    }

    public String getMetricPath() {
        return metricPath;
    }

    public void setMetricPath(String metricPath) {
        this.metricPath = metricPath;
    }

    public String getKeyValueSeparator() {
        return keyValueSeparator;
    }

    public void setKeyValueSeparator(String keyValueSeparator) {
        this.keyValueSeparator = keyValueSeparator;
    }

    public List<String> getMetricsToCollect() {
        return metricsToCollect;
    }

    public void setMetricsToCollect(List<String> metricsToCollect) {
        this.metricsToCollect = metricsToCollect;
    }

    public List<String> getDeltaStats() {
        return deltaStats;
    }

    public void setDeltaStats(List<String> deltaStats) {
        this.deltaStats = deltaStats;
    }
}
