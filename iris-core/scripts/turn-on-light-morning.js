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
 * Date: 18.01.16
 * Time: 11:57
 * This is test script for event engine of IRISv2
 */

var d = new Date();
var hour = d.getHours();

//////////////////////////////////////////////////////

// if device state = ON and device have internalname = noolite/channel/6
if (advertisement.getLabel() == "DeviceOn" && device.getInternalName() == "noolite/channel/6" && hour == 7) {
    LOGGER.info("[morning] Detected movement!");

    // imports
    var Device = Java.type("ru.iris.common.database.model.devices.Device");
    var Lock = Java.type("ru.iris.common.database.Lock");
    var Speak = Java.type("ru.iris.common.helpers.Speak");
    var DeviceCtl = Java.type("ru.iris.common.helpers.DeviceCtl");
    var Timer = Java.type("java.util.Timer");

    var uuid = advertisement.getValue();
    var device = Device.getDeviceByUUID(uuid);

    // lock not set
    if (!Lock.isLocked("morning-light-on")) {
        LOGGER.info("[morning] Lock not set. Setted!");

        // lock task
        var lock = new Lock("morning-light-on");
        lock.lock();
    }
    else {
        // turn on light in bathroom
        DeviceCtl.on("bathroom");
        Speak.say("Доброе утро!");
    }
}

if (advertisement.getLabel() == "DeviceOff" && device.getInternalName() == "noolite/channel/6" && Lock.isLocked("morning-light-on") && hour != 7) {
    // release lock
    Lock.release("morning-light-on");
    LOGGER.info("[morning] Release lock!");
}
