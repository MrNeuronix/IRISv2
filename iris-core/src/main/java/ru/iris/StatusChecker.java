package ru.iris;

import com.avaje.ebean.Ebean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.database.model.ModuleStatus;
import ru.iris.common.messaging.JsonEnvelope;
import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.service.ServiceAdvertisement;
import ru.iris.common.messaging.model.service.ServiceStatus;

import java.sql.Timestamp;
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

    private static Logger log = LogManager.getLogger(StatusChecker.class.getName());

    public StatusChecker() {
        Thread t = new Thread(this);
        t.setName("Status Checker Service");
        t.start();
    }

    @Override
    public synchronized void run() {

        //TODO

    }
}
