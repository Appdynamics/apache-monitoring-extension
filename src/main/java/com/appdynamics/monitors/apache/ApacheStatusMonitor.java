package com.appdynamics.monitors.apache;

import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.DeltaMetricsCalculator;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom, Satish M
 * Date: 4/21/14
 * Time: 7:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApacheStatusMonitor extends AManagedMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|WebServer|Apache|Status|";

    private static final Logger logger = Logger.getLogger(ApacheStatusMonitor.class);

    private static final String CONFIG_ARG = "config-file";

    private boolean initialized;
    private MonitorConfiguration configuration;

    private final DeltaMetricsCalculator deltaCalculator = new DeltaMetricsCalculator(10);


    public ApacheStatusMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    private static String getImplementationVersion() {
        return ApacheStatusMonitor.class.getPackage().getImplementationTitle();
    }

    public TaskOutput execute(Map<String, String> args, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        logger.info("Starting the Apache Monitoring task.");

        Thread thread = Thread.currentThread();
        ClassLoader originalCl = thread.getContextClassLoader();
        thread.setContextClassLoader(AManagedMonitor.class.getClassLoader());

        try {
            if (!initialized) {
                initialize(args);
            }
            configuration.executeTask();

            logger.info("Finished Apache monitor execution");
            return new TaskOutput("Finished Apache monitor execution");
        } catch (Exception e) {
            logger.error("Failed to execute the Apache monitoring task", e);
            throw new TaskExecutionException("Failed to execute the Apache monitoring task" + e);
        } finally {
            thread.setContextClassLoader(originalCl);
        }
    }

    private void initialize(Map<String, String> argsMap) {
        if (!initialized) {
            final String configFilePath = argsMap.get(CONFIG_ARG);

            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            conf.setConfigYml(configFilePath);

            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.METRIC_PREFIX,
                    MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER, MonitorConfiguration.ConfItem.HTTP_CLIENT, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {
            if (!initialized) {
                logger.info("Apache Monitor is still initializing");
                return;
            }

            Map<String, ?> config = configuration.getConfigYml();


            List<Map> apacheServers = (List<Map>) config.get("servers");

            for (Map apacheServer : apacheServers) {

                ApacheMonitoringTask task = new ApacheMonitoringTask(configuration, apacheServer, deltaCalculator);
                configuration.getExecutorService().execute(task);
            }
        }
    }

    public static void main(String[] args) throws TaskExecutionException {

        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level.DEBUG);

        logger.getRootLogger().addAppender(ca);

        final ApacheStatusMonitor monitor = new ApacheStatusMonitor();

        final Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "/Users/Muddam/AppDynamics/Code/extensions/apache-monitoring-extension/src/main/resources/conf/config.yml");

        //monitor.execute(taskArgs, null);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    monitor.execute(taskArgs, null);
                } catch (Exception e) {
                    logger.error("Error while running the task", e);
                }
            }
        }, 2, 30, TimeUnit.SECONDS);
    }
}
