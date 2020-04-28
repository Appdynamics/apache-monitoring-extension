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
import com.appdynamics.extensions.apache.metrics.ServerStats;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.PathResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.CloseableHttpClient;
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HttpClientUtils.class)
@PowerMockIgnore("javax.net.ssl.*")
public class ServerStatsTest {

    public static final Logger logger = ExtensionsLoggerFactory.getLogger(ServerStatsTest.class);

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    @Mock
    private MetricWriteHelper metricWriter;

    @Mock
    private Phaser phaser;

    private Stat.Stats stat;

    private MonitorContextConfiguration contextConfiguration = new MonitorContextConfiguration("Apache", "Custom Metrics|Apache|", PathResolver.resolveDirectory(AManagedMonitor.class), Mockito.mock(AMonitorJob.class));


    private Map<String, String> expectedValueMap;

    private ServerStats serverStats;


    @Before
    public void before(){

        contextConfiguration.setConfigYml("src/test/resources/test-config.yml");
        contextConfiguration.setMetricXml("src/test/resources/test-metrics.xml", Stat.Stats.class);

        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);

        stat = (Stat.Stats) contextConfiguration.getMetricsXml();

        serverStats = Mockito.spy(new ServerStats(stat.getStats()[0], contextConfiguration.getContext(), new HashMap<String, String>(), metricWriter,
                                                    contextConfiguration.getMetricPrefix(), phaser));

        PowerMockito.mockStatic(HttpClientUtils.class);
        PowerMockito.mockStatic(CloseableHttpClient.class);

        PowerMockito.when(HttpClientUtils.getResponseAsLines(any(CloseableHttpClient.class), anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        ObjectMapper mapper = new ObjectMapper();
                        List<String> list = FileUtils.readLines(new File("src/test/resources/server-status.txt"), "utf-8");
                        logger.info("Returning the mocked data for the api ");
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
        map.put("Custom Metrics|Apache|Availability|Server Uptime (sec)","906998");
        map.put("Custom Metrics|Apache|Resource Utilization|CPU|Load",".598118");
        map.put("Custom Metrics|Apache|Resource Utilization|Processes|Busy Workers","371");
        map.put("Custom Metrics|Apache|Resource Utilization|Processes|Idle Workers","397");
        map.put("Custom Metrics|Apache|Resource Utilization|ConnsAsyncClosing","406");
        map.put("Custom Metrics|Apache|Resource Utilization|ConnsAsyncKeepAlive","139");
        map.put("Custom Metrics|Apache|Resource Utilization|ConnsAsyncWriting","13");
        map.put("Custom Metrics|Apache|Resource Utilization|Total Connections","935");
        map.put("Custom Metrics|Apache|Activity|Total Accesses","101625603");
        map.put("Custom Metrics|Apache|Activity|Total Traffic","16067823270");
        map.put("Custom Metrics|Apache|Activity|Requests/min","112.046");
        map.put("Custom Metrics|Apache|Activity|Bytes/min","18140600.41");
        map.put("Custom Metrics|Apache|Activity|Type|Waiting for Conn","397");
        map.put("Custom Metrics|Apache|Activity|Type|Sending Reply","45");
        map.put("Custom Metrics|Apache|Activity|Type|Reading Request","322");
        map.put("Custom Metrics|Apache|HeartBeat", "1");
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
