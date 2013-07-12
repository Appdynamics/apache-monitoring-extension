package com.appdynamics.monitors.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.EnvPropertyWriter;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public abstract class JavaServersMonitor extends AManagedMonitor
{
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
	protected volatile String host;
	protected volatile String port;
	protected volatile String userName;
	protected volatile String passwd;
	protected volatile String serverRoot = "C:/Program Files/Apache Software Foundation/Apache2.2";   // root directory of the server we are monitoring (for finding files or deploying programs)
	protected volatile String restartAllowed = "TRUE";

	protected volatile Map<String, String> oldValueMap;
	protected volatile Map<String, String> valueMap;
	protected volatile long oldTime = 0;
	protected volatile long currentTime = 0;

	public abstract TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
			throws TaskExecutionException;

	protected void parseArgs(Map<String, String> args)
	{ 
		host = getArg(args, "host", "localhost");
		userName = getArg(args, "user", userName);
		passwd = getArg(args, "password", passwd);
		port = getArg(args, "port", "90");
		serverRoot = getArg(args, "serverRoot", serverRoot);
		restartAllowed = getArg(args, "restartAllowed", restartAllowed);
	}

	// safe way to get parameter from monitor, but if null, use default
	protected String getArg(Map<String, String> args,String arg, String oldVal)
	{
		String result = args.get(arg);

		if (result == null)
			return oldVal;
		
		return result;
	}
	
	protected void printStringMetric(String name, String value)
	{
		String metricName = getMetricPrefix() + name;
		EnvPropertyWriter writer = this.getPropertyWriter();
		writer.printEnvironmentProperty(metricName, value);
		
		// just for debug output
		
		if (logger.isDebugEnabled())
		{
			logger.debug("METRIC STRING:  NAME:" + name + " VALUE:" + value);
		}
	}
	
	protected void printMetric(String name, String value, String aggType, String timeRollup, String clusterRollup)
	{
		String metricName = getMetricPrefix() + name;
		MetricWriter metricWriter = getMetricWriter(metricName, aggType, timeRollup, clusterRollup);
		metricWriter.printMetric(value);

		// just for debug output
		if (logger.isDebugEnabled())
		{
			logger.debug("METRIC:  NAME:" + metricName + " VALUE:" + value + " :" + aggType + ":" + timeRollup + ":"
					+ clusterRollup);
		}
	}

	protected String getMetricPrefix()
	{
		return "";
	}

	protected void startExecute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
	{
		valueMap = Collections.synchronizedMap(new HashMap<String, String>());
		parseArgs(taskArguments);
	}

	protected TaskOutput finishExecute()
	{
		oldValueMap = valueMap;
		oldTime = currentTime;

		// just for debug output
		logger.debug("Finished METRIC COLLECTION for Monitor.......");

		return new TaskOutput("Success");
	}

	protected void close(ResultSet rs, Statement stmt, Connection conn)
	{
		if (rs != null)
		{
			try 
			{
				rs.close();
			} 
			catch (Exception e) 
			{
				// ignore
			}
		}
		
		if (stmt != null)
		{
			try 
			{
				stmt.close();
			} 
			catch (Exception e) 
			{
				// ignore
			}
		}
		
		if (conn != null)
		{
			try 
			{
				conn.close();
			} 
			catch (Exception e) 
			{
				// ignore
			}
		}		
	}
	
	protected String getString(float num)
	{
		int result = Math.round(num);
		return Integer.toString(result);
	}

	// lookup value for key, convert to float, round up or down and then return as string form of int
	protected String getString(String key)
	{
		return getString(key, true);
	}
	
	// specify whether to convert this key to uppercase before looking up the value
	protected String getString(String key, boolean convertUpper)
	{
		if (convertUpper)
			key = key.toUpperCase();
		
		String strResult = valueMap.get(key);

		if (strResult == null)
			return "0";

		// round the result to a integer since we don't handle fractions
		float result = Float.valueOf(strResult);
		String resultStr = getString(result);
		return resultStr;
	}

	protected String getPercent(String numerator, String denumerator)
	{
		float tmpResult = 0;

		try
		{
			tmpResult = getValue(numerator) / getValue(denumerator);
		}
		catch (Exception e)
		{
			return null;
		}
		tmpResult = tmpResult * 100;
		return getString(tmpResult);
	}

	protected String getReversePercent(float numerator, float denumerator)
	{
		if (denumerator == 0)
			return null;

		float tmpResult = numerator / denumerator;
		tmpResult = 1 - tmpResult;
		tmpResult = tmpResult * 100;
		return getString(tmpResult);
	}

	protected String getPercent(float numerator, float denumerator)
	{
		if (denumerator == 0)
			return getString(0);

		float tmpResult = numerator / denumerator;
		tmpResult = tmpResult * 100;
		return getString(tmpResult);
	}

	// math utility
	protected float getValue(String key)
	{
		String strResult = valueMap.get(key.toUpperCase());

		if (strResult == null)
			return 0;

		float result = Float.valueOf(strResult);
		return result;
	}

	protected float getDiffValue(String key)
	{
		String strResult = valueMap.get(key.toUpperCase());

		if (strResult == null)
			return 0;

		float result = Float.valueOf(strResult);
		float oldResult = 0;

		String oldResultStr = oldValueMap.get(key.toUpperCase());
		if (oldResultStr != null)
			oldResult = Float.valueOf(oldResultStr);

		return (result - oldResult);
	}

}
