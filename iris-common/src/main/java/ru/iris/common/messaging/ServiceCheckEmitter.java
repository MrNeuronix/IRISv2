package ru.iris.common.messaging;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 14.11.13
 * Time: 17:46
 * License: GPL v3
 */

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.messaging.model.service.ServiceAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;

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

public class ServiceCheckEmitter implements Runnable {

    private Logger log = LogManager.getLogger(ServiceCheckEmitter.class.getName());
    private boolean shutdown = false;
    private UUID instanceId = UUID.randomUUID();
    private boolean reinitialize = false;
    private ServiceAdvertisement advertisement = new ServiceAdvertisement();

    public ServiceCheckEmitter(String moduleName) {
        advertisement.setName(moduleName);
        Thread t = new Thread(this);
        t.setName("Service Check Emitter");
        t.start();
    }

    public void setUUID(UUID instanceId) {
        this.instanceId = instanceId;
    }

    public UUID getUUID() {
        return instanceId;
    }

    public void setState(ServiceStatus state) {
        this.advertisement.setStatus(state);
        this.reinitialize = true;
    }

    @Override
    public void run() {

        try {
            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            JsonMessaging jsonMessaging = new JsonMessaging(instanceId);

            jsonMessaging.broadcast("service.status", advertisement);

            long lastStatusBroadcastMillis = System.currentTimeMillis();

            while (!shutdown) {

                // If there is more than 30 seconds from last availability broadcasts then lets redo this.
                if (30000L < System.currentTimeMillis() - lastStatusBroadcastMillis || reinitialize) {
                    jsonMessaging.broadcast("service.status", advertisement);
                    lastStatusBroadcastMillis = System.currentTimeMillis();
                    if (reinitialize)
                        reinitialize = false;
                }

                // avoids high cpu load
                Thread.sleep(5000L);
            }

            // Broadcast that this service is shutdown.
            jsonMessaging.broadcast("service.status", advertisement);

            // Close JSON messaging.
            jsonMessaging.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in Status Checker", t);
        }
    }
}

