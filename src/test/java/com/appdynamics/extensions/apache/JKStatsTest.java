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
import com.appdynamics.extensions.apache.metrics.JKStats;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.http.HttpClientUtils;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.PathResolver;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.io.FileUtils;
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
public class JKStatsTest {
    public static final Logger logger = LoggerFactory.getLogger(JKStatsTest.class);

    @Mock
    private TasksExecutionServiceProvider serviceProvider;

    @Mock
    private MetricWriteHelper metricWriter;

    @Mock
    private Phaser phaser;

    private Stat.Stats stat;

    private MonitorContextConfiguration monitorConfiguration = new MonitorContextConfiguration("Apache", "Custom Metrics|Apache|", PathResolver.resolveDirectory(AManagedMonitor.class), Mockito.mock(AMonitorJob.class));


    private Map<String, String> expectedValueMap;


    private JKStats jkStats;


    @Before
    public void before(){

        monitorConfiguration.setConfigYml("src/test/resources/test-config.yml");
        monitorConfiguration.setMetricXml("src/test/resources/test-metrics.xml", Stat.Stats.class);

        //Mockito.when(serviceProvider.getMonitorConfiguration()).thenReturn(monitorConfiguration);
        Mockito.when(serviceProvider.getMetricWriteHelper()).thenReturn(metricWriter);

        stat = (Stat.Stats) monitorConfiguration.getMetricsXml();

        jkStats = Mockito.spy(new JKStats(stat.getStats()[1], monitorConfiguration.getContext(), new HashMap<String, String>(), metricWriter,
                monitorConfiguration.getMetricPrefix(), phaser));

        PowerMockito.mockStatic(HttpClientUtils.class);
        PowerMockito.mockStatic(CloseableHttpClient.class);

        PowerMockito.when(HttpClientUtils.getResponseAsLines(any(CloseableHttpClient.class), anyString())).thenAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        ObjectMapper mapper = new ObjectMapper();
                        List<String> list = FileUtils.readLines(new File("src/test/resources/jk-status.txt"), "utf-8");
                        logger.info("Returning the mocked data for the api ");
                        return list;
                    }
                });
    }


    @Test
    public void testServerStats() throws TaskExecutionException {

        expectedValueMap = getExpectedValueMap();
        jkStats.run();

        validateMetrics();
        Assert.assertTrue("The expected values were not send. The missing values are " + expectedValueMap
                , expectedValueMap.isEmpty());
    }
    private Map<String, String> getExpectedValueMap() {
        Map<String, String> map = Maps.newHashMap();
        map.put("Custom Metrics|Apache|worker|worker1|connection_pool_timeout","0");
        map.put("Custom Metrics|Apache|worker|worker2|connection_pool_timeout","0");
        map.put("Custom Metrics|Apache|worker|worker1|ping_timeout","10000");
        map.put("Custom Metrics|Apache|worker|worker2|ping_timeout","10000");
        map.put("Custom Metrics|Apache|worker|worker1|connect_timeout","0");
        map.put("Custom Metrics|Apache|worker|worker2|connect_timeout","0");
        map.put("Custom Metrics|Apache|worker|worker1|prepost_timeout","0");
        map.put("Custom Metrics|Apache|worker|worker2|prepost_timeout","0");
        map.put("Custom Metrics|Apache|worker|worker1|reply_timeout","0");
        map.put("Custom Metrics|Apache|worker|worker2|reply_timeout","0");
        map.put("Custom Metrics|Apache|worker|worker1|retries","2");
        map.put("Custom Metrics|Apache|worker|worker2|retries","2");
        map.put("Custom Metrics|Apache|worker|worker1|connection_ping_interval","0");
        map.put("Custom Metrics|Apache|worker|worker2|connection_ping_interval","0");
        map.put("Custom Metrics|Apache|worker|worker1|recovery_options","0");
        map.put("Custom Metrics|Apache|worker|worker2|recovery_options","0");
        map.put("Custom Metrics|Apache|worker|worker1|max_packet_size","8192");
        map.put("Custom Metrics|Apache|worker|worker2|max_packet_size","8192");
        map.put("Custom Metrics|Apache|worker|worker1|activation","STP");
        map.put("Custom Metrics|Apache|worker|worker2|activation","STP");
        map.put("Custom Metrics|Apache|worker|worker1|lbfactor","100");
        map.put("Custom Metrics|Apache|worker|worker2|lbfactor","100");
        map.put("Custom Metrics|Apache|worker|worker1|lbmult","1");
        map.put("Custom Metrics|Apache|worker|worker2|lbmult","1");
        map.put("Custom Metrics|Apache|worker|worker1|distance","0");
        map.put("Custom Metrics|Apache|worker|worker2|distance","0");
        map.put("Custom Metrics|Apache|worker|worker1|lbvalue","0");
        map.put("Custom Metrics|Apache|worker|worker2|lbvalue","0");
        map.put("Custom Metrics|Apache|worker|worker1|elected","0");
        map.put("Custom Metrics|Apache|worker|worker2|elected","0");
        map.put("Custom Metrics|Apache|worker|worker1|sessions","0");
        map.put("Custom Metrics|Apache|worker|worker2|sessions","0");
        map.put("Custom Metrics|Apache|worker|worker1|errors","0");
        map.put("Custom Metrics|Apache|worker|worker2|errors","0");
        map.put("Custom Metrics|Apache|worker|worker1|client_errors","0");
        map.put("Custom Metrics|Apache|worker|worker2|client_errors","0");
        map.put("Custom Metrics|Apache|worker|worker1|reply_timeouts","0");
        map.put("Custom Metrics|Apache|worker|worker2|reply_timeouts","0");
        map.put("Custom Metrics|Apache|worker|worker1|transferred","0");
        map.put("Custom Metrics|Apache|worker|worker2|transferred","0");
        map.put("Custom Metrics|Apache|worker|worker1|read","0");
        map.put("Custom Metrics|Apache|worker|worker2|read","0");
        map.put("Custom Metrics|Apache|worker|worker1|busy","0");
        map.put("Custom Metrics|Apache|worker|worker2|busy","0");
        map.put("Custom Metrics|Apache|worker|worker1|max_busy","0");
        map.put("Custom Metrics|Apache|worker|worker2|max_busy","0");
        map.put("Custom Metrics|Apache|worker|worker1|connected","0");
        map.put("Custom Metrics|Apache|worker|worker2|connected","0");
        map.put("Custom Metrics|Apache|worker|worker1|time_to_recover_min","0");
        map.put("Custom Metrics|Apache|worker|worker2|time_to_recover_min","0");
        map.put("Custom Metrics|Apache|worker|worker1|time_to_recover_max","0");
        map.put("Custom Metrics|Apache|worker|worker2|time_to_recover_max","0");
        map.put("Custom Metrics|Apache|worker|worker1|used","0");
        map.put("Custom Metrics|Apache|worker|worker2|used","0");
        map.put("Custom Metrics|Apache|worker|worker1|map_count","0");
        map.put("Custom Metrics|Apache|worker|worker2|map_count","0");
        map.put("Custom Metrics|Apache|worker|worker1|last_reset_ago","518");
        map.put("Custom Metrics|Apache|worker|worker2|last_reset_ago","518");

        return map;
    }

    private void validateMetrics(){
        for(Metric metric: jkStats.getMetrics()) {

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

    @Test
    public void testBigDecimalToString() {
        JKStats.toBigDecimal("33227844900").toString().equals("33227844900");
        JKStats.toBigDecimal("332278.900").toString().equals("332278.900");
        JKStats.toBigDecimal("33").toString().equals("33");
    }

    @Test(expected = NullPointerException.class)
    public void testInvalidNumberToBigDecimal() {
        JKStats.toBigDecimal("23asf").toString().equals("23asf");
    }
}
