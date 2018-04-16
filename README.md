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

#### Custom Stats Configuration

  If you have any custom URL's which return delimiter separated metrics, please define them in metrics.xml as following:

    <stat name="customStats">
        <stat url=<URL-of-custom-stats> keyValueSeparator=<Delimiter> >
            <metric attr=<AttributeToMonitor> alias="<PathofMetric>" aggregationType = "OBSERVATION" timeRollUpType = "CURRENT" clusterRollUpType = "COLLECTIVE"/>
        </stat>
    </stat>

#### Instances Configuration

  The extension supports reporting metrics from multiple apache instances. Have a look at config.yml for more details.

  Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ApacheMonitor/`. Below is the format

``` yaml
servers:
   - displayName: "Local Apache"
     host: "localhost"
     port: 80
     username: ""
     password: ""
     useSSL: false

connection:
  socketTimeout: 3000
  connectTimeout: 2500
  sslCertCheckEnabled: false
  sslVerifyHostname: false

proxy:
  uri:
  username:
  password:

numberOfThreads: 5
encryptionKey: "welcome"

#This will create this metric in all the tiers, under this alias
#metricPrefix: Custom Metrics|WebServer|

#This will create it in specific Tier/Component. Make sure to replace <COMPONENT_ID> with the appropriate one from your environment.
#To find the <COMPONENT_ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|WebServer|Apache|Status|

```

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
|Product Tested On         |3.2.0+      |
|Last Update               |04/12/2018  |
