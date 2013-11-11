/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.iris.common.service;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Example service implementation.
 *
 * @author Tommi S.E. Laukkanen
 */
public class ExampleService {
    /**
     * The logger.
     */
    private static Logger LOGGER = LoggerFactory.getLogger(JsonMessaging.class);
    /**
     * If shutdown is ni progress.
     */
    private static boolean shutdown = false;

    /**
     * The main method.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        try {
            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            // Configuring log4j..
            DOMConfigurator.configure("conf/etc/log4j.xml");

            final UUID instanceId;
            if (args.length == 0) {
                // Lets define default instance ID for this type of service.
                instanceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f62ef");
            } else {
                // Lets use the instance ID given as command line argument for this service instance.
                instanceId = UUID.fromString(args[0]);
            }

            // Lets load configuration.
            final Map<String, String> config = new Config().getConfig();

            // Lets instantiate JSON messaging with the selected instance ID for this module using
            // keystore path and password defined in configuration.
            final JsonMessaging jsonMessaging = new JsonMessaging(
                    instanceId, config.get("keystore-path"), config.get("keystore-password"));
            // Lets subscribe to listen service.status subject.
            jsonMessaging.subscribe("service.status");
            // Lets start JSON processing to be able to exchange messages.
            jsonMessaging.start();

            // Broadcast that this service has started up.
            jsonMessaging.broadcast("service.status", new ServiceAdvertisement(
                    "Example Service", instanceId, ServiceStatus.STARTUP,
                    new ServiceCapability[]{ServiceCapability.SPEAK}));

            long lastStatusBroadcastMillis = System.currentTimeMillis();
            while (!shutdown) {

                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessaging.receive(100);
                if (envelope != null) {
                    if (envelope.getObject() instanceof ServiceAdvertisement) {
                        // We know of service advertisement. Lets log it properly.
                        final ServiceAdvertisement serviceAdvertisement = envelope.getObject();
                        LOGGER.info("Service '" + serviceAdvertisement.getType()
                                + "' status: '" + serviceAdvertisement.getStatus()
                                + "' capabilities: " + Arrays.asList(serviceAdvertisement.getCapabilities())
                                + " instance: '" + serviceAdvertisement.getInstanceId()
                                + "'"
                        );
                    } else if (envelope.getReceiverInstanceId() == null) {
                        // We received unknown broadcast message. Lets make generic log entry.
                        LOGGER.info("Received broadcast "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        LOGGER.info("Received request "
                                + " from " + envelope.getSenderInstanceId()
                                + " to " + envelope.getReceiverInstanceId()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }

                // If there is more than 60 seconds from last availability broadcasts then lets redo this.
                if (60000L < System.currentTimeMillis() - lastStatusBroadcastMillis) {
                    jsonMessaging.broadcast("service.status", new ServiceAdvertisement(
                            "Example Service", instanceId, ServiceStatus.AVAILABLE,
                            new ServiceCapability[]{ServiceCapability.SPEAK}));
                    lastStatusBroadcastMillis = System.currentTimeMillis();
                }

            }

            // Broadcast that this service is shutdown.
            jsonMessaging.broadcast("service.status", new ServiceAdvertisement(
                    "Example Service", instanceId, ServiceStatus.SHUTDOWN,
                    new ServiceCapability[]{ServiceCapability.SPEAK}));

            // Close JSON messaging.
            jsonMessaging.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            LOGGER.error("Unexpected exception in example service main method.", t);
        }

    }

}
