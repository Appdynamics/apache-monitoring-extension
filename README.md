# AppDynamics Apache - Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

The Apache HTTP Server is a widely-used web server supported by the Apache Software Foundation. The Apache HTTP Server monitoring extension captures metrics from an Apache web server and displays them in the AppDynamics Metric Browser.

This extension is capable of fetching metrics from most of the apache based HTTP servers. Tested the extension with IBM HTTP server 7.0 and Oracle HTTP Server 12.1.2. 

Metrics include:

   * Availability: Percentage of time that the server has been up; graphs server up/down status over time.
   * Health Check: Most recent and most severe logs, errrors, etc.
   * Activity: Throughput over the last minute including requests, bytes, etc.
   * Resource Utilization: Resources (threads, CPU, memory) currently in use and still available.
   * Efficiency: Server optimizations to maximize throughput such as caches, etc.

In addition, it lists:

   * Top Requests: Most requests by quantity and by volume, measured by number of bytes transferred.
   * Top Activity: Current activity such as responding, cleaning up, logging, etc.

##Prerequisite

Please enable mod_status on the HTTP server to get stats.


##Installation
1. Install Apache mod_status on your Apache instance. For more information, see [Apache Module mod_status](http://httpd.apache.org/docs/2.0/mod/mod_status.html).

2. Use `curl` to verify that the URL works: http://your-apache-server:90/server-status?auto

   ```
   > curl http://localhost:90/server-status?auto
    Total kBytes: 3
    Total Accesses: 3
    Total kBytes: 3
    Uptime: 119503
    ReqPerSec: 2.5104e-5
    BytesPerSec: .0257065
    BytesPerReq: 1024
    BusyWorkers: 1
    IdleWorkers: 7
    Scoreboard: __W___……………….
   ```

3. Run `maven clean install`. Deploy the ApacheMonitor.zip file found in 'target' into the \<machine agent home\>/monitors directory.

   ```
> cd <machine agent home>/monitors/
> unzip ApacheMonitor.zip
   ```
4. Set up config.yml with the correct host and port:
 ```
# Apache particulars

servers:
   - displayName: "Local Apache"
     host: "localhost"
     port: 80
     username: ""
     password: ""
     useSSL: false
     proxyHost: ""
     proxyPort: ""
     proxyUsername: ""
     proxyPassword: ""
     #mod_status stats url path
     statsUrlPath: "/server-status?auto"
     #Only enable this if you have enabled mod_jk module enabled on apache server
     jkStatusPath: ""

     #If you have any custom URL's which return delimiter separated metrics define them here.
     customStats:
        #Metric group under which all the metrics are displayed
       - metricGroup: "MyCustomMetrics"
         #Metric path to get the metrics from
         metricPath: ""
         #Metric key value separator
         keyValueSeparator: ":"
         #Comma separated List of metrics which will be shown in the controller
         #keep this empty if you want to show all the metrics
         metricsToCollect: []
         #Add custom metrics with full path here to get delta for those metrics
         deltaMetrics: []


connection:
  socketTimeout: 5000
  connectTimeout: 2500
  sslCertCheckEnabled: false
  sslVerifyHostname: false

numberOfThreads: 5
encryptionKey: "welcome"

####
## type = AggregationType.TimeRollup.ClusterRollup
## AggregationType = AVG | SUM | OBS
## TimeRollup = AVG | SUM | CUR
## ClusterRollup = IND | COL
##
## https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
####

metrics:
   - serverMetrics:
         - name: "Uptime"
           path: "Availability|Server Uptime (sec)"
           type: "OBS.CUR.COL"
         - name: "CPULoad"
           path: "Resource Utilization|CPU|Load"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "BusyWorkers"
           path: "Resource Utilization|Processes|Busy Workers"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "IdleWorkers"
           path: "Resource Utilization|Processes|Idle Workers"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "ConnsAsyncClosing"
           path: "Resource Utilization|ConnsAsyncClosing"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "ConnsAsyncKeepAlive"
           path: "Resource Utilization|ConnsAsyncKeepAlive"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "ConnsAsyncWriting"
           path: "Resource Utilization|ConnsAsyncWriting"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "ConnsTotal"
           path: "Resource Utilization|Total Connections"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Total Accesses"
           path: "Activity|Total Accesses"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Total kBytes"
           path: "Activity|Total Traffic"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "ReqPerSec"
           path: "Activity|Requests/min"
           multiplier: "60"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "BytesPerSec"
           path: "Activity|Bytes/min"
           multiplier: "60"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "BytesPerReq"
           path: "Activity|Bytes/req"
           type: "OBS.AVG.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"

         #Scoreboard metrics. Do not edit.
         #https://www.apache.org/server-status
         - name: "Waiting for Conn"
           path: "Activity|Type|Waiting for Conn"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Starting Up"
           path: "Activity|Type|Starting Up"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Reading Request"
           path: "Activity|Type|Reading Request"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Sending Reply"
           path: "Activity|Type|Sending Reply"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Keep Alive"
           path: "Activity|Type|Keep Alive"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "DNS Lookup"
           path: "Activity|Type|DNS Lookup"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Closing Connection"
           path: "Activity|Type|Closing Connection"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Logging"
           path: "Activity|Type|Logging"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Gracefully Finishing"
           path: "Activity|Type|Gracefully Finishing"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "Cleaning Up"
           path: "Activity|Type|Cleaning Up"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
   - jkMetrics:
         - name: "connection_pool_timeout"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "ping_timeout"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "connect_timeout"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "prepost_timeout"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "reply_timeout"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "retries"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "connection_ping_interval"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "recovery_options"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "max_packet_size"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "activation"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "lbfactor"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "distance"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "lbmult"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "lbvalue"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "elected"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "sessions"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "errors"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "client_errors"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "reply_timeouts"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "transferred"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "read"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "busy"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "max_busy"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "connected"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "time_to_recover_min"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "time_to_recover_max"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "used"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "map_count"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"
         - name: "last_reset_ago"
           path: "JK Status"
           type: "OBS.CUR.COL"
           collectDelta: "true"
           deltaType: "OBS.CUR.COL"


#This will create this metric in all the tiers, under this path
#metricPrefix: Custom Metrics|WebServer|Apache|Status|

#This will create it in specific Tier/Component. Make sure to replace <COMPONENT_ID> with the appropriate one from your environment.
#To find the <COMPONENT_ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|WebServer|Apache|Status|

```

5. Restart the Machine Agent.

Output from this monitoring extension includes:

-   Counter Description CPU Load: The percentage of the CPU used by Apache.

-   Requests/Sec: The number of HTTP requests the web server is processing per second.

-   Bytes/Sec: The amount of data the web server is transferring per second.

-   Bytes/Requests Average: The average number of bytes being transferred per HTTP request.

-   Busy Workers: The number of Apache processes actively processing an HTTP request.

-   Idle Workers: The number of idle Apache processes waiting for an HTTP request.

##Directory Structure

<table><tbody>
<tr>
<th align = 'left'> Directory/File </th>
<th align = 'left'> Description </th>
</tr>
<tr>
<td class='confluenceTd'> src/main/resources/config </td>
<td class='confluenceTd'> Contains monitor.xml and config.yml</td>
</tr>
<tr>
<td class='confluenceTd'> src/main/java </td>
<td class='confluenceTd'> Contains source code to the Apache monitoring extension </td>
</tr>
<tr>
<td class='confluenceTd'> target </td>
<td class='confluenceTd'> Only obtained when using maven. Run 'maven clean install' to get the distributable .zip file. </td>
</tr>
<tr>
<td class='confluenceTd'> pom.xml </td>
<td class='confluenceTd'> maven build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>


##Metrics

###Availability

-   Uptime (1 or 0)

###Resource Utilization

-   Counter Description CPU Load (N/A on Windows) -- The percentage of the CPU used by Apache.
-   Processes
    - Busy Workers -- The number of Apache processes actively processing an HTTP request.
    - Idle Workers -- The number of idle Apache processes waiting for an HTTP request.
-   Memory

###Activity

-   Accesses -- Total number of accesses per Minute
-   Total Traffic (kb)
-   Requests per second -- The number of HTTP requests the web server is processing per second.
-   Bytes per second -- The amount of data the web server is transferring per second.
-   Bytes per request -- The average number of bytes being transferred per HTTP request.
-   Activity Types
    -   Starting up
    -   Reading Request
    -   Sending Reply
    -   Keepalive
-   DNS Lookup
-   Closing Connection
-   Logging
-   Gracefully Finishing
-   Cleaning up of working

##Load balancing metrics

In addition to the above specified metrics, this extension can also show metrics from mod_jk status. To do this we have to configure mod_jk in the apache HTTP server.
More info on mod_jk at http://tomcat.apache.org/connectors-doc/

###example configuration

####in httpd.conf
   ```   
	LoadModule    jk_module  modules/mod_jk.so
	JkWorkersFile conf/workers.properties
	JkShmFile     /var/log/httpd/mod_jk.shm
	JkLogFile     /var/log/httpd/mod_jk.log
	JkLogLevel    info
	JkLogStampFormat "[%a %b %d %H:%M:%S %Y] "
	JkMount  /examples/* loadbalancer
	
	<Location /status>
	    JkMount statusmanager
	    Order deny,allow
	    Allow from localhost
	</Location>

   ```
####worker.properties file	
   ```
	worker.list=worker1,worker2,loadbalancer,statusmanager
	
	#worker1
	worker.worker1.type=ajp13
	worker.worker1.host={host1}
	worker.worker1.port={host1 ajp post}
	worker.worker1.lbfactor=100   
	
	#worker2
	worker.worker2.type=ajp13
	worker.worker2.host={host2}
	worker.worker2.port={host2 ajp port}
	worker.worker2.lbfactor=100   
	
	#Load balancer
	worker.loadbalancer.type=lb
	worker.loadbalancer.balance_workers=worker1,worker2
	worker.loadbalancer.sticky_session=1
	
	#status manager
	worker.statusmanager.type=status
	
   ```
####config.yml
   ```
   #Only enable this if you have enabled mod_jk module enabled on apache server
     jkStatusPath: "/status"
     
   ```
   
##Custom Metrics

You can monitor custom URLs which return name-value pairs configured in apache.
example custom URL configuration in config.yml

```
customStats:
        #Metric group under which all the metrics are displayed
    -   metricGroup: "MyCustom"
        #Metric path to get the metrics from
        metricPath: "/test1.html"
        #Metric key value separator
        keyValueSeparator: ":"
        #Comma separated List of metrics which will be shown in the controller
        #keep this empty if you want to show all the metrics
        metricsToCollect: ["key1", "key2"]
        #Add custom metrics with full path here to get delta for those metrics
        deltaStats: ["key1"]
```



##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/apache-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/Extensions/Apache-Monitoring-Extension/idi-p/753) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
