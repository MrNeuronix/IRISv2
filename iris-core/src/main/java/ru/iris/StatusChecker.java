package ru.iris;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.SQL;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.ServiceAdvertisement;
import ru.iris.common.messaging.model.ServiceCapability;
import ru.iris.common.messaging.model.ServiceStatus;

import java.util.Arrays;
import java.util.UUID;

/**
 * Copyright 2013 Tommi S.E. Laukkanen, Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class StatusChecker implements Runnable {

    private static Logger log = LoggerFactory.getLogger(StatusChecker.class.getName());
    private static boolean shutdown = false;
    private SQL sql;
    private static ServiceAdvertisement advertisement = new ServiceAdvertisement();

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

            sql = new SQL();

            final UUID instanceId = UUID.fromString("444b3e75-7c0c-4d6e-a1f3-f373ef7f6501");
            final JsonMessaging jsonMessagingStatus = new JsonMessaging(instanceId);

            jsonMessagingStatus.subscribe("service.status");
            jsonMessagingStatus.start();

            // Broadcast that this service has started up.
            jsonMessagingStatus.broadcast("service.status", advertisement.set(
                    "Status Checker", instanceId, ServiceStatus.STARTUP));

            long lastStatusBroadcastMillis = System.currentTimeMillis();
            while (!shutdown) {

                // Lets wait for 100 ms on json messages and if nothing comes then proceed to carry out other tasks.
                final JsonEnvelope envelope = jsonMessagingStatus.receive(100);
                if (envelope != null) {
                    if (envelope.getObject() instanceof ServiceAdvertisement) {
                        // We know of service advertisement. Lets log it properly.
                        final ServiceAdvertisement serviceAdvertisement = envelope.getObject();

                        log.debug("Service '" + serviceAdvertisement.getName()
                                + "' status: '" + serviceAdvertisement.getStatus()
                                + " instance: '" + serviceAdvertisement.getInstanceId()
                                + "'"
                        );

                        sql.doQuery("DELETE FROM modulestatus WHERE name='" + serviceAdvertisement.getName() + "'");
                        sql.doQuery("INSERT INTO modulestatus (name, lastseen, state) VALUES ('" + serviceAdvertisement.getName() + "',NOW(),'" + serviceAdvertisement.getStatus() + "')");

                    } else if (envelope.getReceiverInstance() == null) {
                        // We received unknown broadcast message. Lets make generic log entry.
                        log.debug("Received broadcast "
                                + " from " + envelope.getSenderInstance()
                                + " to " + envelope.getReceiverInstance()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    } else {
                        // We received unknown request message. Lets make generic log entry.
                        log.debug("Received request "
                                + " from " + envelope.getSenderInstance()
                                + " to " + envelope.getReceiverInstance()
                                + " at '" + envelope.getSubject()
                                + ": " + envelope.getObject());
                    }
                }

                // If there is more than 30 seconds from last availability broadcasts then lets redo this.
                if (30000L < System.currentTimeMillis() - lastStatusBroadcastMillis) {
                    jsonMessagingStatus.broadcast("service.status", advertisement.set(
                            "Status Checker", instanceId, ServiceStatus.AVAILABLE));
                    lastStatusBroadcastMillis = System.currentTimeMillis();
                }

            }

            // Broadcast that this service is shutdown.
            jsonMessagingStatus.broadcast("service.status", advertisement.set(
                    "Status Checker", instanceId, ServiceStatus.SHUTDOWN));

            // Close JSON messaging.
            jsonMessagingStatus.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in Status Checker", t);
        }

    }
}
