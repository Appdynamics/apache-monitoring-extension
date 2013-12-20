package com.appdynamics.monitors.apache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.appdynamics.monitors.common.JavaServersMonitor;
import com.singularity.ee.util.httpclient.HttpClientWrapper;
import com.singularity.ee.util.httpclient.HttpExecutionRequest;
import com.singularity.ee.util.httpclient.HttpExecutionResponse;
import com.singularity.ee.util.httpclient.HttpOperation;
import com.singularity.ee.util.httpclient.IHttpClientWrapper;
import com.singularity.ee.util.log4j.Log4JLogger;

public class ApacheStatusMonitor extends JavaServersMonitor
{

    Logger logger = Logger.getLogger(ApacheStatusMonitor.class.getName());
    
	private final static String metricPrefix = "Custom Metrics|WebServer|Apache|Status|";

	public ApacheStatusMonitor()
	{
		oldValueMap = new HashMap<String, String>();
	}

	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
			throws TaskExecutionException
	{
		startExecute(taskArguments, taskContext);

		try
		{
			populate(valueMap, null);
		}
		catch (IOException e)
		{
			throw new TaskExecutionException(e);
		}
		catch (IllegalAccessException e)
		{
			throw new TaskExecutionException(e);
		}
		catch (ClassNotFoundException e)
		{
			throw new TaskExecutionException(e);
		}
		catch (InstantiationException e)
		{
			throw new TaskExecutionException(e);
		}

        if(logger.isDebugEnabled()) {
            logger.debug("Starting METRIC COLLECTION for Apache Monitor.......");
        }

		// Availability
		printMetric("Availability|up", getString(1),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		// printStringMetric("Availability|Server Uptime (sec)", getString("Uptime"));
		printMetric("Availability|Server Uptime (sec)", getString("Uptime"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

		// RESOURCE UTILIZATION
		
		printMetric("Resource Utilization|CPU|Load", getString("CPULoad"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		printMetric("Resource Utilization|Processes|Busy Workers", getString("BusyWorkers"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		printMetric("Resource Utilization|Processes|Idle Workers", getString("IdleWorkers"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		// Activity
		printMetric("Activity|Total Accesses", getString(getDiffValue("Total Accesses")),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
		
		printMetric("Activity|Total Traffic", getString(getDiffValue("Total kBytes")),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Requests/min", getString("ReqPerMin"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Bytes/min", getString("BytesPerMin"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Bytes/req", getString(getDiffValue("BytesPerReq")),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|Starting Up",  getString("StartingUp"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|Reading Request",  getString("ReadingRequest"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|Sending Reply",  getString("SendingReply"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|Keep Alive",  getString("KeepAlive"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|DNS Lookup",  getString("DNSLookup"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|Closing Connection",  getString("ClosingConnection"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|Logging",  getString("Logging"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|Gracefully Finishing",  getString("GracefullyFinishing"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		printMetric("Activity|Type|Cleaning Up",  getString("CleaningUp"),
				MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);

		return finishExecute();
	}

	// collects all monitoring data for this time period from database
	protected void populate(Map<String, String> valueMap, String arg1) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException
	{
		
		IHttpClientWrapper httpClient = HttpClientWrapper.getInstance();

		// make http request for stats to apache
		String connStr = "http://" + host + ":" + port + customURLPath;
		HttpExecutionRequest request = new HttpExecutionRequest(connStr, "", HttpOperation.GET);
		HttpExecutionResponse response = httpClient.executeHttpOperation(request, new Log4JLogger(logger));

        if (response.isExceptionHappened()) {
            logger.error("Failed to execute HTTP request to URL " + connStr +". Got error"+ response.getExceptionMessage());
            throw new RuntimeException(response.getExceptionMessage());
        }

		String statStr = response.getResponseBody();

		// get most accurate time
		currentTime = System.currentTimeMillis();

        logger.error(statStr); //Print metrics to log

		BufferedReader reader = new BufferedReader(new StringReader(statStr));
		String line;
		
		// time to parse
		while ((line = reader.readLine()) != null)
		{
			Pattern p = Pattern.compile(":", Pattern.LITERAL);
			String[] result = p.split(line);

			if (result.length == 2)
			{
				String key = result[0];
				String value = result[1];

				if (key.equals("Scoreboard")) {
					parseScoreboard(value);
				}
				else 
				{
					// convert from secends to minutes since our system is minute based and these numbers could always show up as zero
					if (key.equals("BytesPerSec"))
					{
						key = "BytesPerMin";
						float floatVal = Float.valueOf(value);
						value = getString(floatVal * 60);
					}
					if (key.equals("ReqPerSec"))
					{
						key = "ReqPerMin";
						float floatVal = Float.valueOf(value);
						value = getString(floatVal * 60);
					}

					valueMap.put(key.toUpperCase(), value);
				}
			}
		}
	}

	// go through and count each activity type
	protected void 	parseScoreboard(String value) throws IOException
	{
		int StartingUp, ReadingRequest, SendingReply,KeepAlive, DNSLookup,ClosingConnection, Logging,GracefullyFinishing, CleaningUp;
		StartingUp = ReadingRequest = SendingReply=KeepAlive=DNSLookup=ClosingConnection=Logging=GracefullyFinishing= CleaningUp = 0;

		StringReader reader = new StringReader(value);
		
		char typeChar;
		int typeInt;
		while ((typeInt = reader.read()) != -1)
		{
			typeChar = (char)typeInt;
			
			switch (typeChar)
			{
			case 'I':
				CleaningUp++;
				break;
			case 'C':
				ClosingConnection++;
				break;
			case 'S': 
				StartingUp++;
				break;
			case 'R':
				ReadingRequest++;
				break;
			case 'W':
				SendingReply++;
				break;
			case 'K':
				KeepAlive++;
				break;
			case 'D':
				DNSLookup++;
				break;
			case 'L':
				Logging++;
				break;
			case 'G':
				GracefullyFinishing++;
				break;
			default: 
				break;
			
			}
		}
		
		valueMap.put("STARTINGUP", Integer.toString(StartingUp));
		valueMap.put("READINGREQUEST", Integer.toString(ReadingRequest));
		valueMap.put("SENDINGREPLY", Integer.toString(SendingReply));
		valueMap.put("KEEPALIVE", Integer.toString(KeepAlive));
		valueMap.put("DNSLOOKUP", Integer.toString(DNSLookup));
		valueMap.put("CLOSINGCONNECTION", Integer.toString(ClosingConnection));
		valueMap.put("LOGGING", Integer.toString(Logging));
		valueMap.put("GRACEFULLYFINISHING", Integer.toString(GracefullyFinishing));
		valueMap.put("CLEANINGUP", Integer.toString(CleaningUp));
		
		reader.close();

	}
	
	protected String getMetricPrefix()
	{
		return this.metricPrefix;
	}

	public static void main(String[] args) throws Exception
	{
		ApacheStatusMonitor monitor = new ApacheStatusMonitor();
		Map<String, String> taskArguments = new HashMap<String, String>();
		taskArguments.put("port", "90");
		TaskExecutionContext taskContext = null;

		monitor.execute(taskArguments, taskContext);
	}
}
