/*
 * Copyright 2012-2014 Nikolay A. Viguro
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

package ru.iris.common.helpers;

import ru.iris.common.messaging.JsonMessaging;
import ru.iris.common.messaging.model.speak.ChangeVolumeAdvertisement;

import java.util.UUID;

/**
 * User: nikolay.viguro
 * Date: 02.02.15
 * Time: 12:28
 */
public class VolumeCtl {
    private static final JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());

    public static void on() {
        on("all");
    }

    public static void off() {
        off("all");
    }

    public static void on(String device) {
        messaging.broadcast("event.speak.volume.set", new ChangeVolumeAdvertisement(100D, device));
    }

    public static void off(String device) {
        messaging.broadcast("event.speak.volume.set", new ChangeVolumeAdvertisement(0D, device));
    }

    public static void level(String device, double level) {
        messaging.broadcast("event.speak.volume.set", new ChangeVolumeAdvertisement(level, device));
    }
}
