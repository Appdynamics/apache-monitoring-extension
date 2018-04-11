/*
 *
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.apache;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.apache.input.Stat;
import com.appdynamics.extensions.apache.metrics.MetricsUtil;
import com.appdynamics.extensions.apache.metrics.ServerStats;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HttpClientUtils.class)
@PowerMockIgnore("javax.net.ssl.*")
public class ServerStatsTest {

    public static final Logger logger = LoggerFactory.getLogger(ServerStatsTest.class);

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    @Mock
    private MetricWriteHelper metricWriter;

    @Mock
    private Phaser phaser;

    private Stat.Stats stat;

    private MonitorConfiguration monitorConfiguration = new MonitorConfiguration("Apache", "Custom Metrics|Apache|", Mockito.mock(AMonitorJob.class));


    private Map<String, String> expectedValueMap;

    private ServerStats serverStats;


    @Before
    public void before(){

        monitorConfiguration.setConfigYml("src/test/resources/test-config.yml");
        monitorConfiguration.setMetricsXml("src/test/resources/test-metrics.xml", Stat.Stats.class);

        Mockito.when(serviceProvider.getMonitorConfiguration()).thenReturn(monitorConfiguration);
        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);

        stat = (Stat.Stats) monitorConfiguration.getMetricsXmlConfiguration();

        serverStats = Mockito.spy(new ServerStats(stat.getStats()[0], monitorConfiguration, new HashMap<String, String>(), metricWriter,
                                                    monitorConfiguration.getMetricPrefix(), phaser));

        PowerMockito.mockStatic(HttpClientUtils.class);
        PowerMockito.mockStatic(CloseableHttpClient.class);

        PowerMockito.when(HttpClientUtils.getResponseAsLines(any(CloseableHttpClient.class), anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        ObjectMapper mapper = new ObjectMapper();
                        String file = "src/test/resources/server-status.txt";
                        List<String> list = new ArrayList<String>();
                        try (BufferedReader br = Files.newBufferedReader(Paths.get(file))) {
                            list = br.lines().collect(Collectors.toList());
                        } catch (IOException e) {
                            logger.error("Error reading file : ", e);
                        }
                        logger.info("Returning the mocked data for the api " + file);
                        return list;
                    }
                });
    }


    @Test
    public void testServerStats() throws TaskExecutionException {

        expectedValueMap = getExpectedValueMap();
        serverStats.run();

        validateMetrics();
        Assert.assertTrue("The expected values were not send. The missing values are " + expectedValueMap
                , expectedValueMap.isEmpty());
    }

    private Map<String, String> getExpectedValueMap() {
        Map<String, String> map = Maps.newHashMap();
        map.put("Custom Metrics|WebServer|Apache|Status|Availability|Server Uptime (sec)","906998");
        map.put("Custom Metrics|WebServer|Apache|Status|Resource Utilization|CPU|Load",".598118");
        map.put("Custom Metrics|WebServer|Apache|Status|Resource Utilization|Processes|Busy Workers","371");
        map.put("Custom Metrics|WebServer|Apache|Status|Resource Utilization|Processes|Idle Workers","397");
        map.put("Custom Metrics|WebServer|Apache|Status|Resource Utilization|ConnsAsyncClosing","406");
        map.put("Custom Metrics|WebServer|Apache|Status|Resource Utilization|ConnsAsyncKeepAlive","139");
        map.put("Custom Metrics|WebServer|Apache|Status|Resource Utilization|ConnsAsyncWriting","13");
        map.put("Custom Metrics|WebServer|Apache|Status|Resource Utilization|Total Connections","935");
        map.put("Custom Metrics|WebServer|Apache|Status|Activity|Total Accesses","101625603");
        map.put("Custom Metrics|WebServer|Apache|Status|Activity|Total Traffic","16067823270");
        map.put("Custom Metrics|WebServer|Apache|Status|Activity|Requests/min","112.046");
        map.put("Custom Metrics|WebServer|Apache|Status|Activity|Bytes/min","18140600.41");
        map.put("Custom Metrics|WebServer|Apache|Status|Activity|Type|Waiting for Conn","397");
        map.put("Custom Metrics|WebServer|Apache|Status|Activity|Type|Sending Reply","45");
        map.put("Custom Metrics|WebServer|Apache|Status|Activity|Type|Reading Request","322");
        return map;
    }

    private void validateMetrics(){
        for(Metric metric: serverStats.getMetrics()) {

            String actualValue = metric.getMetricValue();
            String metricName = metric.getMetricPath();
            if (expectedValueMap.containsKey(metricName)) {
                String expectedValue = expectedValueMap.get(metricName);
                Assert.assertEquals("The value of the metric " + metricName + " failed", expectedValue, actualValue);
                expectedValueMap.remove(metricName);
            } else {
                System.out.println("\"" + metricName + "\",\"" + actualValue + "\"");
                Assert.fail("Unknown Metric " + metricName);
            }
        }
    }

}