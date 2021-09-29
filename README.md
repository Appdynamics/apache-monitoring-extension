# AppDynamics Monitoring Extension for use with Apache

## Use Case

The Apache HTTP Server is a widely-used web server supported by the Apache Software Foundation. The Apache HTTP Server monitoring extension captures metrics from an Apache web server and displays them in the AppDynamics Metric Browser.

## Prerequisite

1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.

2. Download and install [Apache Maven](https://maven.apache.org/) which is configured with `Java 8` to build the extension artifact from source. You can check the java version used in maven using command `mvn -v` or `mvn --version`. If your maven is using some other java version then please download java 8 for your platform and set JAVA_HOME parameter before starting maven.

3. Please enable mod_status on the HTTP server to get stats. Install Apache mod_status on your Apache instance. For more information, see [Apache Module mod_status](http://httpd.apache.org/docs/2.0/mod/mod_status.html).

4. The extension needs to be able to connect to Apache in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product, or have an agent on the same machine running the product in order for the extension to collect and send the metrics.

## Installation
1. Clone the "apache-monitoring-extension" repo using `git clone <repoUrl>` command.
2. Run 'mvn clean install' from "apache-monitoring-extension"
3. Unzip the `ApacheMonitor-<Version>.zip` from `target` directory to the "<MachineAgent_Dir>/monitors" directory
4. Edit the file config.yml as described below in Configuration Section, located in <MachineAgent_Dir>/monitors/ApacheMonitor and update the Apache server(s) details.
5. All metrics to be reported are configured in metrics.xml. Users can remove entries from metrics.xml to stop the metric from reporting, or add new entries as well.
6. Restart the Machine Agent

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.
 
## Configuration

#### Load balancing metrics

This extension can also show the load balancing metrics from mod_jk status. In order to do this, please configure mod_jk in the apache HTTP server.
More info on mod_jk is available [here](http://tomcat.apache.org/connectors-doc/)

Following are the sample configuration files that need to be setup for mod_jk metrics. Please check this [link](http://tomcat.apache.org/connectors-doc/common_howto/quick.html) for more details.

### httpd.conf

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

### worker.properties file

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

#### Config.yml

Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/ApacheMonitor/`.

1. Configure the "COMPONENT_ID" under which the metrics need to be reported. This can be done by changing the value of `<COMPONENT_ID>` in `metricPrefix: "Server|Component:<COMPONENT_ID>|Custom Metrics|Apache|"`.

For example,
```
       metricPrefix: "Server|Component:100|Custom Metrics|Apache|"
```
More details around metric prefix can be found [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695)

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
         useSSL: false

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
For example, if number of servers that need to be monitored is 3, then number of threads required is 3 * 4 = 12
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
| aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)    |
| timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)   |
| clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)|
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

### Server status metrics
Please refer to metrics.xml file located at `<MachineAgentInstallationDirectory>/monitors/ApacheMonitor/metrics.xml` to view the metrics which this extension can report.

### Load balancing metrics:
 This extension can also show metrics from mod_jk status. To do this we have to configure mod_jk in the apache HTTP server. More info on mod_jk can be found [here](http://tomcat.apache.org/connectors-doc/)

## Credentials Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting
1. Please enable mod_status on the HTTP server to get stats. For more information, see [Apache Module mod_status](http://httpd.apache.org/docs/2.0/mod/mod_status.html).
2. Use `curl` to verify that the URL works: http://your-apache-server:90/server-status?auto

```
   > curl -v http://localhost:90/server-status?auto
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

3. Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.

## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/apache-monitoring-extension/).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.0.4       |
|Product Tested On         |4.5.12+     |
|Last Update               |04/01/2021  |
|Changes list              |[ChangeLog](https://github.com/Appdynamics/apache-monitoring-extension/blob/master/CHANGELOG.md)|

**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.
