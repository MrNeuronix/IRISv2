package ru.iris;

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
 * Copyright 2013 Tommi S.E. Laukkanen, Nikolay A. Viguro
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

public class StatusChecker implements Runnable {

    private static Logger log = LoggerFactory.getLogger(StatusChecker.class.getName());
    private static boolean shutdown = false;

    public StatusChecker() {
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

                    final UUID instanceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6001");

                     // Lets load configuration.
                    final Map<String, String> config = new Config().getConfig();

                    // Lets instantiate JSON messaging with the selected instance ID for this module using
                    // keystore path and password defined in configuration.
                    final JsonMessaging jsonMessaging = new JsonMessaging(instanceId);
                    // Lets subscribe to listen service.status subject.
                    jsonMessaging.subscribe("service.status");
                    // Lets start JSON processing to be able to exchange messages.
                    jsonMessaging.start();

                    // Broadcast that this service has started up.
                    jsonMessaging.broadcast("service.status", new ServiceAdvertisement(
                            "Status Checker", instanceId, ServiceStatus.STARTUP,
                            new ServiceCapability[]{ServiceCapability.SYSTEM}));

                    long lastStatusBroadcastMillis = System.currentTimeMillis();
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

                                Launcher.sql.doQuery("DELETE FROM MODULESTATUS WHERE NAME='" + serviceAdvertisement.getName() + "'");
                                Launcher.sql.doQuery("INSERT INTO MODULESTATUS (NAME, LASTSEEN) VALUES ('" + serviceAdvertisement.getName() + "',NOW())");

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

                        // If there is more than 60 seconds from last availability broadcasts then lets redo this.
                        if (60000L < System.currentTimeMillis() - lastStatusBroadcastMillis) {
                            jsonMessaging.broadcast("service.status", new ServiceAdvertisement(
                                    "Status Checker", instanceId, ServiceStatus.AVAILABLE,
                                    new ServiceCapability[]{ServiceCapability.SYSTEM}));
                            lastStatusBroadcastMillis = System.currentTimeMillis();
                        }

                    }

                    // Broadcast that this service is shutdown.
                    jsonMessaging.broadcast("service.status", new ServiceAdvertisement(
                            "Status Checker", instanceId, ServiceStatus.SHUTDOWN,
                            new ServiceCapability[]{ServiceCapability.SYSTEM}));

                    // Close JSON messaging.
                    jsonMessaging.close();

                } catch (final Throwable t) {
                    t.printStackTrace();
                    log.error("Unexpected exception in Status Checker", t);
                }

            }
}
