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
   ~~~
# Apache particulars

host: "localhost"
port: 8989
username: ""
password: ""
useSSL: false
proxyHost: ""
proxyPort: ""
proxyUsername: ""
proxyPassword: ""
customUrlPath: "/server-status?auto"
#Add apache metrics with full path here to get delta for those metrics
deltaStats: ["Custom Metrics|WebServer|Apache|Status|Availability|Server Uptime (sec)", "Custom Metrics|WebServer|Apache|Status|Activity|Total Accesses", "Custom Metrics|WebServer|Apache|Status|Activity|Bytes/min", "Custom Metrics|WebServer|Apache|Status|Activity|Type|DNS Lookup"]
jkStatusPath: "/status"
jkWorkerStats: "connection_pool_timeout,ping_timeout,connect_timeout,prepost_timeout,reply_timeout,retries,connection_ping_interval,recovery_options,max_packet_size,activation,lbfactor,distance,lbmult,lbvalue,elected,sessions,errors,client_errors,reply_timeouts,transferred,read,busy,max_busy,connected,time_to_recover_min,time_to_recover_max,used,map_count,last_reset_ago"
#Add jkstats metrics with full path here to get delta for those metrics
jkDeltaStats: ["Custom Metrics|WebServer|Apache|Status|JK Status|worker|worker1|retries"]
customStats:
        #Metric group under which all the metrics are displayed
    -   metricGroup: ""
        #Metric path to get the metrics from
        metricPath: ""
        #Metric key value separator
        keyValueSeparator: ":"
        #Comma separated List of metrics which will be shown in the controller
        #keep this empty if you want to show all the metrics
        metricsToCollect: []
        #Add custom metrics with full path here to get delta for those metrics
        deltaStats: []

#prefix used to show up metrics in AppDynamics
metricPrefix:  "Custom Metrics|WebServer|Apache|Status|"

   ~~~

   Note: If you want to monitor more than one server, see [Monitoring multiple Apache servers](#Monitoring multiple Apache servers).  

​5. Restart the Machine Agent.

Output from this monitoring extension includes:

-   Counter Description CPU Load: The percentage of the CPU used by Apache.

-   Requests/Sec: The number of HTTP requests the web server is processing per second.

-   Bytes/Sec: The amount of data the web server is transferring per second.

-   Bytes/Requests Average: The average number of bytes being transferred per HTTP request.

-   Busy Workers: The number of Apache processes actively processing an HTTP request.

-   Idle Workers: The number of idle Apache processes waiting for an HTTP request.

### Monitoring multiple Apache servers
Currently, the Apache HTTP Server monitoring extension only supports a single Apache server.
However, you can "fool" the system into monitoring multiple servers as follows.

​1. For each server you want to monitor, copy the monitors/ApacheMonitor directory into another directory, such as monitors/ApacheStatusMonitor2.

​2. Edit the monitor.xml and config.yml files in that directory, changing the `<name>`, protocol, host, port and metric-prefix values accordingly:


   ```  
# Apache particulars

host: "localhost"
port: 8092
username: ""
password: ""
useSSL: false
proxyHost: ""
proxyPort: ""
proxyUsername: ""
proxyPassword: ""
customUrlPath: "/server-status?auto"
#Add apache metrics with full path here to get delta for those metrics
deltaStats: ["Custom Metrics|WebServer|Apache|Status|Availability|Server Uptime (sec)", "Custom Metrics|WebServer|Apache|Status|Activity|Total Accesses", "Custom Metrics|WebServer|Apache|Status|Activity|Bytes/min", "Custom Metrics|WebServer|Apache|Status|Activity|Type|DNS Lookup"]
jkStatusPath: "/status"
jkWorkerStats: "connection_pool_timeout,ping_timeout,connect_timeout,prepost_timeout,reply_timeout,retries,connection_ping_interval,recovery_options,max_packet_size,activation,lbfactor,distance,lbmult,lbvalue,elected,sessions,errors,client_errors,reply_timeouts,transferred,read,busy,max_busy,connected,time_to_recover_min,time_to_recover_max,used,map_count,last_reset_ago"
#Add jkstats metrics with full path here to get delta for those metrics
jkDeltaStats: ["Custom Metrics|WebServer|Apache|Status|JK Status|worker|worker1|retries"]

customStats:
        #Metric group under which all the metrics are displayed
    -   metricGroup: "MyCustom"
        #Metric path to get the metrics from
        metricPath: "/server-status?auto"
        #Metric key value separator
        keyValueSeparator: ":"
        #Comma separated List of metrics which will be shown in the controller
        #keep this empty if you want to show all the metrics
        metricsToCollect: []
        #Add custom metrics with full path here to get delta for those metrics
        deltaStats: []

#prefix used to show up metrics in AppDynamics
metricPrefix:  "Custom Metrics|WebServer|Apache2|Status|"

   ```
​
<br/>
   
​4. When you have finished adding all the servers you want to monitor, restart the machine agent.



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
jkStatusPath: "/status"
jkWorkerStats: "connection_pool_timeout,ping_timeout,connect_timeout,prepost_timeout,reply_timeout,retries,connection_ping_interval,recovery_options,max_packet_size,activation,lbfactor,distance,lbmult,lbvalue,elected,sessions,errors,client_errors,reply_timeouts,transferred,read,busy,max_busy,connected,time_to_recover_min,time_to_recover_max,used,map_count,last_reset_ago"
#Add jkstats metrics with full path here to get delta for those metrics
jkDeltaStats: ["Custom Metrics|WebServer|Apache|Status|JK Status|worker|worker1|retries"]

   ```
   `jkStatusPath` is the url path defined in the Location section of httpd.conf.<br/>
   `jkWorkerStats` list of worker stats.<br/>
   `jkDeltaStats` To capture delta stats for metrics, add them here.

##Custom Metrics

You can monitor custom URLs configured in apache.
example custom URL configuration in config.yml

~~~
customStats:
        #Metric group under which all the metrics are displayed
    -   metricGroup: "MyCustom"
        #Metric path to get the metrics from
        metricPath: "/server-status?auto"
        #Metric key value separator
        keyValueSeparator: ":"
        #Comma separated List of metrics which will be shown in the controller
        #keep this empty if you want to show all the metrics
        metricsToCollect: []
        #Add custom metrics with full path here to get delta for those metrics
        deltaStats: []
~~~



##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/apache-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/Extensions/Apache-Monitoring-Extension/idi-p/753) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
