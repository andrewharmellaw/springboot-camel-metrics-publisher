package com.capgemini.camel.metrics.publisher;

import com.codahale.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Class to publish various Tomcat metrics to Codahale metrics registry
 *
 * @author Ganga Aloori
 */
public class TomcatMetricsPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatMetricsPublisher.class);

    /**
     * Creates a new {@link JmxAttributeGauge} instance for the given JMX object
     * and attribute names.
     *
     * @param objectName Name of the JMX object
     * @param attributeName Name of the JMX attribute
     * @return a {@link JmxAttributeGauge} instance for the given JMX object and
     * attribute names
     */
    private static JmxAttributeGauge getJmxAttributeGauge(String objectName, String attributeName) {
        JmxAttributeGauge jmxAttributeGauge = null;
        try {
            jmxAttributeGauge = new JmxAttributeGauge(ObjectName.getInstance(objectName), attributeName);
        } catch (MalformedObjectNameException | NullPointerException ex) {
            LOGGER.error("Exception while creating JmxAttributeGauge for {} and {}. "
                    + "Exception details: {}", objectName, attributeName, ex.getMessage());
        }
        return jmxAttributeGauge;
    }

    /**
     * Returns a new {@link Builder} for {@link TomcatMetricsPublisher}.
     *
     * @param registry Codahale metric registry to report the metrics to
     * @return a {@link Builder} instance for a {@link TomcatMetricsPublisher}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link TomcatMetricsPublisher} instances.
     */
    public static class Builder {

        private String serverPort;
        private final MetricRegistry registry;
        private final JmxReporter jmxReporter;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.jmxReporter = JmxReporter.forRegistry(registry)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .build();
            this.serverPort = String.valueOf(8080);
        }

        /**
         * Uses the given server port no in Tomcat JMX object names as per the
         * default configuration.
         *
         * @param port Tomcat server port
         * @return a {@link Builder} instance for {@link TomcatMetricsPublisher}
         */
        public Builder serverPortIs(int port) {
            this.serverPort = String.valueOf(port);
            return this;
        }

        /**
         * Starts the {@link JmxReporter} of the current
         * {@link TomcatMetricsPublisher} instance
         *
         */
        public void start() {
            this.registry.register(name("tomcat"), new ThreadPoolMetricSet());
            this.registry.register(name("tomcat"), new RequestProcessorMetricSet());
            this.jmxReporter.start();
        }

        /**
         * Tomcat connector thread pool metric set
         */
        class ThreadPoolMetricSet implements MetricSet {

            private final String PREFIX = "thread-pool";
            private final String THREAD_POOL_OBJECT = MessageFormat.format("Tomcat:type=ThreadPool,name=\"http-nio-{0}\"", serverPort);

            @Override
            public Map<String, Metric> getMetrics() {
                final Map<String, Metric> gauges = new HashMap<>();
                gauges.put(name(PREFIX, "currentThreadCount"), getJmxAttributeGauge(THREAD_POOL_OBJECT, "currentThreadCount"));
                gauges.put(name(PREFIX, "currentThreadsBusy"), getJmxAttributeGauge(THREAD_POOL_OBJECT, "currentThreadsBusy"));
                gauges.put(name(PREFIX, "connectionCount"), getJmxAttributeGauge(THREAD_POOL_OBJECT, "connectionCount"));
                return gauges;
            }
        }

        /**
         * Tomcat request metric set
         */
        class RequestProcessorMetricSet implements MetricSet {

            private final String PREFIX = "request-processor";
            private final String REQUEST_PROCESSOR_OBJECT = MessageFormat.format("Tomcat:type=GlobalRequestProcessor,name=\"http-nio-{0}\"", serverPort);

            @Override
            public Map<String, Metric> getMetrics() {
                final Map<String, Metric> gauges = new HashMap<>();
                gauges.put(name(PREFIX, "maxTime"), getJmxAttributeGauge(REQUEST_PROCESSOR_OBJECT, "maxTime"));
                gauges.put(name(PREFIX, "requestCount"), getJmxAttributeGauge(REQUEST_PROCESSOR_OBJECT, "requestCount"));
                gauges.put(name(PREFIX, "errorCount"), getJmxAttributeGauge(REQUEST_PROCESSOR_OBJECT, "errorCount"));
                return gauges;
            }
        }
    }
}