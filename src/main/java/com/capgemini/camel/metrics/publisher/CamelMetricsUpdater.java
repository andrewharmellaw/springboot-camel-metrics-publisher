package com.capgemini.camel.metrics.publisher;

import com.codahale.metrics.MetricRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.management.event.ExchangeCompletedEvent;
import org.apache.camel.management.event.ExchangeFailedEvent;
import org.apache.camel.management.event.ExchangeRedeliveryEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.EventObject;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Class to capture and output Route level metrics to Codahale.
 *
 * Based on class from https://gist.github.com/aldrinleal/8262171
 */
@Component("camelMetricsUpdater")
public class CamelMetricsUpdater extends EventNotifierSupport {

    Logger LOGGER = LoggerFactory.getLogger(CamelMetricsUpdater.class);

    @Autowired
    MetricRegistry metrics;

    @Override
    public void notify(EventObject event) throws Exception {
        boolean covered = false;

        if (event instanceof AbstractExchangeEvent) {
            AbstractExchangeEvent ev = AbstractExchangeEvent.class.cast(event);

            final Exchange exchange = ev.getExchange();
            String metricPrefix = exchange.getFromRouteId();

            // if we can't find the prefix for the metrics then don't capture any
            if (metricPrefix == null || metricPrefix.equals("")) { return; }

            if (ev instanceof ExchangeCompletedEvent || ev instanceof ExchangeFailedEvent || ev instanceof ExchangeRedeliveryEvent) {
                onExchangeCompletedEvent(ev, metricPrefix);
                covered = true;
            } else {
                metrics.meter(name(event.getClass(), metricPrefix)).mark();
            }
        }

        if (!covered)
            LOGGER.debug("Not covered: Type {} ({})", event.getClass(), event);
    }

    protected void onExchangeCompletedEvent(AbstractExchangeEvent event, String metricPrefix) {
        Period p = new Period(event.getExchange().getProperty(Exchange.CREATED_TIMESTAMP, Date.class).getTime(), System.currentTimeMillis());

        metrics.timer(name(event.getClass(), metricPrefix)).update(p.getMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isEnabled(EventObject event) {
        return true;
    }

    @Override
    protected void doStart() throws Exception {
        setIgnoreCamelContextEvents(true);
        setIgnoreExchangeEvents(false);
        setIgnoreExchangeCreatedEvent(true);
        setIgnoreExchangeRedeliveryEvents(true);
        setIgnoreExchangeSendingEvents(true);
        setIgnoreExchangeSentEvents(true);
        setIgnoreRouteEvents(true);
        setIgnoreServiceEvents(true);
    }
}
