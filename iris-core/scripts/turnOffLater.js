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
 * Date: 10.10.14
 * Time: 16:43
 * This is test script for event engine of IRISv2
 */

// imports
var Device = Java.type("ru.iris.common.database.model.devices.Device");
var Lock = Java.type("ru.iris.common.database.Lock");
var Speak = Java.type("ru.iris.common.helpers.Speak");
var DeviceCtl = Java.type("ru.iris.common.helpers.DeviceCtl");
var Timer = Java.type("java.util.Timer");

//////////////////////////////////////////////////////

// advertisement
var uuid = advertisement.getValue();
var device = Device.getDeviceByUUID(uuid);

// if device state = ON and device have internalname = noolite/channel/4
if (advertisement.getLabel() == "DeviceOn" && device.getInternalName() == "noolite/channel/4") {
    LOGGER.info("[turnOffLater] Device ON!");

    // lock not set
    if (!Lock.isLocked("toilet-light-on")) {
        LOGGER.info("[turnOffLater] Lock not set. Run timer");

        // lock task
        var lock = new Lock("toilet-light-on");
        lock.lock();

        var timer = new Timer("turnOffLaterTimer", true);

        // turn off past 20 minutes
        timer.schedule(function () {
            if (Lock.isLocked("toilet-light-on")) {

                LOGGER.info("[turnOffLater] Times up! Release lock and turn off device " + reluuid);

                // release lock
                Lock.release("toilet-light-on");

                // turn off device, toilet - pseudoname of device (configured in web interface)
                DeviceCtl.off("toilet");

                // lets speak!
                Speak.say("Кто-то опять забыл выключить свет! Прошло 20 минут, выключаю сам");
            }
        }, 1200000);

    }
}

if (advertisement.getLabel() == "DeviceOff" && device.getInternalName() == "noolite/channel/4" && Lock.isLocked("toilet-light-on")) {
    // release lock
    Lock.release("toilet-light-on");
    LOGGER.info("[turnOffLater] Release lock!");
}
