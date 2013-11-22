package ru.iris.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.I18N;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import java.util.Arrays;

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

            jsonMessaging.subscribe("event.status");
            jsonMessaging.subscribe("event.devices.setvalue");
            jsonMessaging.subscribe("event.devices.getinventory");
            jsonMessaging.subscribe("event.devices.responseinventory");
            jsonMessaging.subscribe("event.speak");
            jsonMessaging.subscribe("event.speak.recognized");

            jsonMessaging.start();

            Service.serviceChecker.setAdvertisment(new ServiceAdvertisement(
                    "Events", Service.serviceId, ServiceStatus.AVAILABLE,
                    new ServiceCapability[]{ServiceCapability.CONTROL, ServiceCapability.SENSE}));

            while (!shutdown) {

                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessaging.receive(100);
                if (envelope != null) {
                    if (envelope.getObject() instanceof ServiceAdvertisement) {
                        // We know of service advertisement. Lets log it properly.
                        final ServiceAdvertisement serviceAdvertisement = envelope.getObject();
                        log.info("Service '" + serviceAdvertisement.getName()
                                + "' status: '" + serviceAdvertisement.getStatus()
                                + "' capabilities: " + Arrays.asList(serviceAdvertisement.getCapabilities())
                                + " instance: '" + serviceAdvertisement.getInstanceId()
                                + "'"
                        );

                    } else if (envelope.getReceiverInstanceId() == null) {
                        // We received unknown broadcast message. Lets make generic log entry.
                        log.info("Received broadcast "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        log.info("Received request "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }
            }

            // Broadcast that this service is shutdown.
            Service.serviceChecker.setAdvertisment(new ServiceAdvertisement(
                    "Events", Service.serviceId, ServiceStatus.SHUTDOWN,
                    new ServiceCapability[]{ServiceCapability.SYSTEM}));

            // Close JSON messaging.
            jsonMessaging.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in Events", t);
        }

    }
}
