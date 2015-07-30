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
var Device = Java.type("ru.iris.common.database.model.devices.Device");
var Speak = Java.type("ru.iris.common.helpers.Speak");

var label = advertisement.getValue("label");
var value = advertisement.getValue("data");
var uuid = advertisement.getValue("uuid");

    var device = Device.getDeviceByUUID(uuid);

// if flood state = ON and device have internalname = zwave/alarmsensor/2
    if (label == "Flood" && value == "255" && device.getInternalName() == "zwave/alarmsensor/2") {

        LOGGER.info("[floodSensor] Flood detected!");

        // lets speak!
        Speak.say("Внимание! Обнаружена протечка воды!");
    }


    if (label == "Flood" && value == "0" && device.getInternalName() == "zwave/alarmsensor/2") {

        LOGGER.info("[floodSensor] Flood off!");

        // lets speak!
        Speak.say("Внимание! Протечка воды устранена!");
    }
