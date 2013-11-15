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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iris.common.Config;

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

public class ServiceChecker {

    private static Logger log = LoggerFactory.getLogger(ServiceChecker.class.getName());
    private static boolean shutdown = false;

    public static void start(final UUID instanceId, final Object ServiceAdv) {

        try {
            // Make sure we exit the wait loop if we receive shutdown signal.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            final JsonMessaging jsonMessaging = new JsonMessaging(instanceId);
            // Lets subscribe to listen service.status subject.
            jsonMessaging.subscribe("service.status");
            // Lets start JSON processing to be able to exchange messages.
            jsonMessaging.start();

            // Broadcast that this service has started up.
            jsonMessaging.broadcast("service.status", ServiceAdv);

            long lastStatusBroadcastMillis = System.currentTimeMillis();
            while (!shutdown) {

                // If there is more than 60 seconds from last availability broadcasts then lets redo this.
                if (60000L < System.currentTimeMillis() - lastStatusBroadcastMillis) {
                    jsonMessaging.broadcast("service.status", ServiceAdv);
                    lastStatusBroadcastMillis = System.currentTimeMillis();
                }

            }

            // Broadcast that this service is shutdown.
            jsonMessaging.broadcast("service.status", ServiceAdv);

            // Close JSON messaging.
            jsonMessaging.close();

        } catch (final Throwable t) {
            t.printStackTrace();
            log.error("Unexpected exception in Status Checker", t);
        }

    }
}
