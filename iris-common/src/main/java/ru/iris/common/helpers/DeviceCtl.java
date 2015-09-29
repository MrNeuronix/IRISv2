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
import ru.iris.common.messaging.model.devices.GenericAdvertisement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * User: nikolay.viguro
 * Date: 10.09.13
 * Time: 10:49
 */
public class DeviceCtl
{
	private static final JsonMessaging messaging = new JsonMessaging(UUID.randomUUID());

	public static void on(String uuid) {
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", uuid);
        params.put("label", "Level");
        params.put("data", 255);
        messaging.broadcast("event.devices.setvalue", new GenericAdvertisement("DeviceOn", params));
    }

	public static void off(String uuid)
	{
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", uuid);
        params.put("label", "Level");
        params.put("data", 0);
        messaging.broadcast("event.devices.setvalue", new GenericAdvertisement("DeviceOff", params));
    }
}
