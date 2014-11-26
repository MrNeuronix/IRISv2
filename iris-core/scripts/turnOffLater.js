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

/**
 * @author Nikolay A. Viguro
 * Date: 10.10.14
 * Time: 16:43
 * This is test script for event engine of IRISv2
 */

// importing all classes in package (like import ru.iris.common.* in java)
var CollectionsAndFiles = new JavaImporter(
    Packages.ru.iris.common,
    Packages.ru.iris.common.helpers,
    Packages.ru.iris.common.messaging,
    Packages.ru.iris.common.database,
    Packages.ru.iris.common.messaging.model,
    Packages.ru.iris.common.database.model.devices);

with (CollectionsAndFiles) {

    // setTimeout implementation
    var Platform = Java.type("javafx.application.Platform");
    var Timer = Java.type("java.util.Timer");

    function setTimeout(func, milliseconds) {
        // New timer, run as daemon so the application can quit
        var timer = new Timer("setTimeout", true);
        timer.schedule(function () Platform.runLater(func), milliseconds);
        return timer;
    }

    // advertisement
    var label = advertisement.getLabel();
    var value = advertisement.getValue();
    var uuid = advertisement.getDeviceUUID();

    var device = Device.getDeviceByUUID(uuid);

    // if device state = ON and device have internalname = noolite/channel/4
    if (label == "Level" && value == "255" && device.getInternalName() == "noolite/channel/4") {
        LOGGER.info("[turnOffLater] Device ON!");

        // lock not set
        if (!Lock.isLocked("toilet-light-on")) {
            LOGGER.info("[turnOffLater] Lock not set. Run timer");

            // lock task
            var lock = new Lock("toilet-light-on");
            var reluuid = uuid;
            lock.lock();

            // run in separate thread
            obj = {
                run: function () {
                    function turnOff() {

                        if (Lock.isLocked("toilet-light-on")) {

                            LOGGER.info("[turnOffLater] Times up! Release lock and turn off device " + reluuid);

                            // release lock
                            Lock.release("toilet-light-on");

                            // turn off device
                            new DeviceCtl().off(reluuid);

                            // lets speak!
                            new Speak().say("Кто-то опять забыл выключить свет! Прошло 20 минут, выключаю сам");
                        }
                    }

                    // turn off past 20 minutes
                    setTimeout(turnOff, 1200000);
                }
            };
            obj.run();
        }
    }


    if (label == "Level" && value == "0" && device.getInternalName() == "noolite/channel/4" && Lock.isLocked("toilet-light-on")) {
        // release lock
        Lock.release("toilet-light-on");
        LOGGER.info("[turnOffLater] Release lock!");
    }

}