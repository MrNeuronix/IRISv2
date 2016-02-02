/*
 * Copyright 2012-2016 Nikolay A. Viguro
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

/**
 * @author Nikolay A. Viguro
 * Date: 02.02.16
 * Time: 20:49
 * This is test script for event engine of IRISv2
 */

// importing all classes in package (like import ru.iris.common.* in java)

if (advertisement.getLabel() == "DeviceBright" || advertisement.getLabel() == "DeviceDim") {
    var Device = Java.type("ru.iris.common.database.model.devices.Device");
    var DeviceCtl = Java.type("ru.iris.common.helpers.DeviceCtl");

    var uuid = advertisement.getValue();
    var device = Device.getDeviceByUUID(uuid);

    if (device.getInternalName() == "noolite/channel/3") {
        if (advertisement.getLabel() == "DeviceBright") {
            LOGGER.info("[turn-on-daylight] Turn on kitchen daylight");
            DeviceCtl.on("daylightkitchen");
        }
        else {
            LOGGER.info("[turn-on-daylight] Turn off kitchen dayligh");
            DeviceCtl.off("daylightkitchen");
        }
    }
}