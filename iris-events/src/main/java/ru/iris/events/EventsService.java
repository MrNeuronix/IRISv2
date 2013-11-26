package ru.iris.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.ServiceStatus;

/**
 * Created with IntelliJ IDEA.
 * Author: Nikolay A. Viguro
 * Date: 19.11.13
 * Time: 11:25
 * License: GPL v3
 */

public class EventsService implements Runnable {

    private Logger log = LoggerFactory.getLogger(EventsService.class.getName());
    private final I18N i18n = new I18N();
    private boolean shutdown = false;

    public EventsService() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public synchronized void run() {

        try {
            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            final JsonMessaging jsonMessaging = new JsonMessaging(Service.serviceId);

            jsonMessaging.subscribe("event.*");
            jsonMessaging.start();

            Service.serviceChecker.setAdvertisment(Service.advertisement.set(
                    "Events", Service.serviceId, ServiceStatus.AVAILABLE));

            while (!shutdown) {

                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessaging.receive(100);
                if (envelope != null) {

                    log.info("EVENT DETECTED! " + envelope.getObject());

                }
            }

            // Broadcast that this service is shutdown.
            Service.serviceChecker.setAdvertisment(Service.advertisement.set(
                    "Events", Service.serviceId, ServiceStatus.SHUTDOWN));

            // Close JSON messaging.
            jsonMessaging.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in Events", t);
        }

    }
}
