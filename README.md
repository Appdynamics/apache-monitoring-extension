# AppDynamics Monitoring Extension for use with Apache

## Use Case

The Apache HTTP Server is a widely-used web server supported by the Apache Software Foundation. The Apache HTTP Server monitoring extension captures metrics from an Apache web server and displays them in the AppDynamics Metric Browser.

## Prerequisite

Please enable mod_status on the HTTP server to get stats. Install Apache mod_status on your Apache instance. For more information, see [Apache Module mod_status](http://httpd.apache.org/docs/2.0/mod/mod_status.html).

In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Java+Agent) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).  For more details on downloading these products, please  visit [here](https://download.appdynamics.com/).

The extension needs to be able to connect to Apache in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product, or have an agent on the same machine running the product in order for the extension to collect and send the metrics.

## Installation
1. Download and unzip the ApacheMonitor.zip to the "<MachineAgent_Dir>/monitors" directory
2. Edit the file config.yml as described below in Configuration Section, located in <MachineAgent_Dir>/monitors/ApacheMonitor and update the Apache server(s) details.
3. All metrics to be reported are configured in metrics.xml. Users can remove entries from metrics.xml to stop the metric from reporting, or add new entries as well.
4. Restart the Machine Agent

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.

## Configuration

#### Load balancing metrics

This extension can also show the load balancing metrics from mod_jk status. In order to do this, please configure mod_jk in the apache HTTP server.
More info on mod_jk is available [here](http://tomcat.apache.org/connectors-doc/)

Following are the sample configuration files that need to be setup for mod_jk metrics. Please check this [link](http://tomcat.apache.org/connectors-doc/common_howto/quick.html) for more details.

### httpd.conf
   ~~~
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

   ~~~
### worker.properties file
   ~~~
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

   ~~~

#### Config.yml

Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ApacheMonitor/`.

  1. Configure the "COMPONENT_ID" under which the metrics need to be reported. This can be done by changing the value of `<COMPONENT_ID>` in
       metricPrefix: "Server|Component:<COMPONENT_ID>|Custom Metrics|Apache|".

       For example,
       ```
       metricPrefix: "Server|Component:100|Custom Metrics|Apache|"

  2. The extension supports reporting metrics from multiple apache instances. Have a look at config.yml for more details.

      For example:
      ```
      servers:
       - displayName: "Local Apache"
         host: "localhost"
         port: 80
         username: ""
         password: ""
         encryptedPassword: ""
         useSsl: false

      connection:
        socketTimeout: 5000
        connectTimeout: 2500
        sslCertCheckEnabled: true
        sslVerifyHostname: false
      ```
  3. Configure the encyptionKey for encryptionPasswords(only if password encryption required).
     For example,
     ```
     #Encryption key for Encrypted password.
     encryptionKey: "axcdde43535hdhdgfiniyy576"
     ```
  4. Configure the numberOfThreads
     For example,
     If number of servers that need to be monitored is 3, then number of threads required is 3 * 4 = 12
     ```
     numberOfThreads: 12
     ```
#### Metrics.xml

You can add/remove metrics of your choosing by modifying the provided metrics.xml file. This file consists of all the metrics that
will be monitored and sent to the controller. Please look at how the metrics have been defined and follow the same convention when
adding new metrics. You do have the ability to also chose your Rollup types as well as for each metric as well as set an alias name
that you would like displayed on the metric browser.

This monitor provides an option to add a custom URL for monitoring the metrics provided by the particular end-point.
##### Custom Stats Configuration
 If you have any custom URL's which return delimiter separated metrics, please define them in metrics.xml as following:
 ```
 <stat name="customStats">
     <stat url=<URL-of-custom-stats> keyValueSeparator=<Delimiter> >
        <metric attr=<AttributeToMonitor> alias="<PathofMetric>" aggregationType = "OBSERVATION" timeRollUpType = "CURRENT" clusterRollUpType = "COLLECTIVE"/>
     </stat>
 </stat>
 ```
For configuring the metrics, the following properties can be used:

     |     Property      |   Default value |         Possible values         |                                              Description                                                                                                |
     | :---------------- | :-------------- | :------------------------------ | :------------------------------------------------------------------------------------------------------------- |
     | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
     | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)    |
     | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)   |
     | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
     | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
     | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:0, DOWN:1  |
     | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |

     For example,
     ```
     - name: "CPUUtilization"
              alias: "CPULoad"
              aggregationType: "OBSERVATION"
              timeRollUpType: "CURRENT"
              clusterRollUpType: "COLLECTIVE"
              delta: false
     ```
     **All these metric properties are optional, and the default value shown in the table is applied to the metric(if a property has not been specified) by default.**

## Metrics

### Availability:
 Uptime (1 or 0)

### Resource Utilization:
     |     Property                |     Description                                                             |                                                                                              |
     | :-------------------------- | :-------------------------------------------------------------------------- |
     | CPU Load (N/A on Windows)   | The substitute name to be used in the metric browser instead of metric name.|                                   |
     | Processes                   |                                                                             |
     | Busy Workers                | The number of Apache processes actively processing an HTTP request          |
     | Idle Workers                | The number of idle Apache processes waiting for an HTTP request.            |
     | Total Connections                      |                                                                             |

### Activity:
     |     Property                |     Description                                                             |                                                                                              |
     | :-------------------------- | :-------------------------------------------------------------------------- |
     | Accesses                    | Total number of accesses per Minute                                         |
     | Total Traffic (kb)          |                                                                             |
     | Requests per second         | The number of HTTP requests the web server is processing per second.        |
     | Bytes per second            | The amount of data the web server is transferring per second.               |
     | Bytes per request           | The average number of bytes being transferred per HTTP request.             |
     | Activity Types              |                                                                             |
     | Starting up                 |                                                                             |
     | Reading Request             |                                                                             |
     | Sending Reply               |                                                                             |
     | Keepalive                   |                                                                             |
     | DNS Lookup                  |                                                                             |
     | Closing Connection          |                                                                             |
     | Logging                     |                                                                             |
     | Gracefully Finishing        |                                                                             |
     | Cleaning up of working      |                                                                             |

### Load balancing metrics:
 In addition to the above specified metrics, this extension can also show metrics from mod_jk status. To do this we have to
 configure mod_jk in the apache HTTP server. More info on mod_jk can be found [here](http://tomcat.apache.org/connectors-doc/)



## Credentials Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting
1. Please enable mod_status on the HTTP server to get stats. For more information, see [Apache Module mod_status](http://httpd.apache.org/docs/2.0/mod/mod_status.html).
2. Use `curl` to verify that the URL works: http://your-apache-server:90/server-status?auto

  ~~~
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
  ~~~

3. Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to contact the support team.

## Support Tickets
If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.

    1. Stop the running machine agent.
    2. Delete all existing logs under <MachineAgent>/logs.
    3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug.
        <logger name="com.singularity">
        <logger name="com.appdynamics">
    4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
    5. Attach the zipped <MachineAgent>/conf/* directory here.
    6. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory here.

For any support related questions, you can also contact help@appdynamics.com.


## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/apache-monitoring-extension/).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.0.0       |
|Controller Compatibility  |3.7 or Later|
|Product Tested On         |2.4.33      |
|Last Update               |05/14/2018  |
