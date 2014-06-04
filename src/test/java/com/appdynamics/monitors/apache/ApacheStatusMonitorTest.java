package com.appdynamics.monitors.apache;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.WebTarget;
import com.appdynamics.extensions.io.Lines;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/21/14
 * Time: 11:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApacheStatusMonitorTest {
    public static final Logger logger = LoggerFactory.getLogger(ApacheStatusMonitorTest.class);

    @Test
    public void testAll() throws TaskExecutionException {
        Lines lines = new Lines(getClass().getResourceAsStream("/status.txt"));
        SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
        WebTarget target = Mockito.mock(WebTarget.class);
        Response response = Mockito.mock(Response.class);
        Mockito.when(httpClient.target(Mockito.anyString())).thenReturn(target);
        Mockito.when(target.get()).thenReturn(response);
        Mockito.when(response.lines()).thenReturn(lines);
        final HashMap<String, String> expectedValueMap = getExpectedValueMap();
        ApacheStatusMonitor spy = Mockito.spy(new ApacheStatusMonitor());
        Mockito.doReturn(httpClient).when(spy).buildHttpClient(Mockito.anyMap());
        Mockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String metricPrefix = (String) invocation.getArguments()[0];
                String value = (String) invocation.getArguments()[1];
                String expected = expectedValueMap.get(metricPrefix);
                logger.debug("{} expected={}, actual = {}",metricPrefix,expected,value);
                Assert.assertEquals(expected, value);
                expectedValueMap.remove(metricPrefix);
                return null;
            }
        }).when(spy).printMetric(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        HashMap<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("metric-prefix","Apache| ");
        spy.execute(argsMap, null);
        logger.info("The remaining values are {}",expectedValueMap);
        Assert.assertEquals(0,expectedValueMap.size());
    }

    @Test(expected = TaskExecutionException.class)
    public void testException() throws TaskExecutionException {
        SimpleHttpClient httpClient = Mockito.mock(SimpleHttpClient.class);
        WebTarget target = Mockito.mock(WebTarget.class);
        Response response = Mockito.mock(Response.class);
        Mockito.when(httpClient.target(Mockito.anyString())).thenReturn(target);
        Mockito.when(target.get()).thenReturn(response);
        Mockito.when(response.lines()).thenThrow(new RuntimeException());
        HashMap<String, String> argsMap = new HashMap<String, String>();
        ApacheStatusMonitor spy = Mockito.spy(new ApacheStatusMonitor());
        Mockito.doReturn(httpClient).when(spy).buildHttpClient(Mockito.anyMap());
        spy.execute(argsMap, null);
    }

    private HashMap<String, String> getExpectedValueMap() {
        HashMap<String, String> map = Maps.newHashMap();
        map.put("Apache|Availability|up","1");
        map.put("Apache|Resource Utilization|Processes|Busy Workers","371");
        map.put("Apache|Activity|Total Traffic","16067823270");
        map.put("Apache|Availability|Server Uptime (sec)","906998");
        map.put("Apache|Resource Utilization|ConnsAsyncWriting","13");
        map.put("Apache|Resource Utilization|Processes|Idle Workers","397");
        map.put("Apache|Resource Utilization|CPU|Load","1");
        map.put("Apache|Activity|Total Accesses","101625603");
        map.put("Apache|Resource Utilization|ConnsAsyncClosing","406");
        map.put("Apache|Resource Utilization|Total Connections","935");
        map.put("Apache|Resource Utilization|ConnsAsyncKeepAlive","139");
        map.put("Apache|Activity|Bytes/min","1088436025");
        map.put("Apache|Activity|Requests/min","6723");
        map.put("Apache|Activity|Type|Sending Reply","45");
        map.put("Apache|Activity|Type|Gracefully Finishing","122");
        map.put("Apache|Activity|Type|Reading Request","322");
        map.put("Apache|Activity|Type|Logging","10");
        map.put("Apache|Activity|Type|Waiting for Conn","397");
        map.put("Apache|Activity|Bytes/req","161904");
        return map;
    }
}
