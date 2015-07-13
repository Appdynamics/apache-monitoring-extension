package com.appdynamics.monitors.apache.config;


import java.util.List;

public class Configuration {

    private String host;
    private int port;
    private String username;
    private String password;
    private boolean useSSL;
    private String proxyHost;
    private String proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private String customUrlPath;
    private List<String> deltaStats;
    private String metricPrefix;
    private String jkStatusPath;
    private String jkWorkerStats;
    private List<String> jkDeltaStats;

    private List<CustomStats> customStats;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public String getCustomUrlPath() {
        return customUrlPath;
    }

    public void setCustomUrlPath(String customUrlPath) {
        this.customUrlPath = customUrlPath;
    }

    public List<String> getDeltaStats() {
        return deltaStats;
    }

    public void setDeltaStats(List<String> deltaStats) {
        this.deltaStats = deltaStats;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public String getJkStatusPath() {
        return jkStatusPath;
    }

    public void setJkStatusPath(String jkStatusPath) {
        this.jkStatusPath = jkStatusPath;
    }

    public String getJkWorkerStats() {
        return jkWorkerStats;
    }

    public void setJkWorkerStats(String jkWorkerStats) {
        this.jkWorkerStats = jkWorkerStats;
    }

    public List<String> getJkDeltaStats() {
        return jkDeltaStats;
    }

    public void setJkDeltaStats(List<String> jkDeltaStats) {
        this.jkDeltaStats = jkDeltaStats;
    }

    public List<CustomStats> getCustomStats() {
        return customStats;
    }

    public void setCustomStats(List<CustomStats> customStats) {
        this.customStats = customStats;
    }
}
