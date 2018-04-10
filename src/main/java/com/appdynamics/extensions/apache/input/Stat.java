/*
 *
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.apache.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Stat {
    @XmlAttribute
    private String url;
    @XmlAttribute
    private String name;
    @XmlAttribute(name = "filter-name")
    private String filterName;
    @XmlAttribute(name = "keyValueSeparator")
    private String keyValueSeparator;
    @XmlAttribute(name = "metric-type")
    private String metricType;
    @XmlAttribute
    public String children;
    @XmlElement(name = "metric")
    private MetricConfig[] metricConfig;
    @XmlElement(name = "naming")
    private Naming naming;
    @XmlElement(name = "stat")
    public Stat[] stats;


    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MetricConfig[] getMetricConfig() {
        return metricConfig;
    }

    public void setMetricConfig(MetricConfig[] metricConfig) {
        this.metricConfig = metricConfig;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Naming getNaming() {
        return naming;
    }

    public void setNaming(Naming naming) {
        this.naming = naming;
    }

    public Stat[] getStats() {
        return stats;
    }

    public void setStats(Stat[] stats) {
        this.stats = stats;
    }

    public String getChildren() {
        return children;
    }

    public void setChildren(String children) {
        this.children = children;
    }

    public String getKeyValueSeparator() {
        return keyValueSeparator;
    }

    public void setKeyValueSeparator(String keyValueSeparator) {
        this.keyValueSeparator = keyValueSeparator;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Stats {
        @XmlElement(name = "stat")
        private Stat[] stats;

        public Stat[] getStats() {
            return stats;
        }

        public void setStats(Stat[] stats) {
            this.stats = stats;
        }
    }
}
