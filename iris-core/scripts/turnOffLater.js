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

// imports
var Device = Java.type("ru.iris.common.database.model.devices.Device");
var Lock = Java.type("ru.iris.common.database.Lock");
var Speak = Java.type("ru.iris.common.helpers.Speak");
var Timer = Java.type('java.util.Timer');
var Phaser = Java.type('java.util.concurrent.Phaser');

// setTimeout implementation
//////////////////////////////////////////////////////

var timer = new Timer('jsEventLoop', false);
var phaser = new Phaser();
var canceled = false;

var onTaskFinished = function () {
    phaser.arriveAndDeregister();
};

function setTimeout(fn, millis /* [, args...] */) {

    var args = [].slice.call(arguments, 2, arguments.length);
    var phase = phaser.register();

    timer.schedule(function () {
        if (canceled) {
            return;
        }

        try {
            fn.apply(args);
        } catch (e) {
            print(e);
        } finally {
            onTaskFinished();
        }
    }, millis);

    return function () {
        onTaskFinished();
        canceled = true;
    };
    }

function clearTimeout(cancel) {
    onTaskFinished();
    canceled = true;
}

//////////////////////////////////////////////////////

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
