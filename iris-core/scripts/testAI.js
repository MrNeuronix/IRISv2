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
 * Date: 27.11.13
 * Time: 12:19
 * This is test script for event engine of IRISv2
 */

// importing all classes in package (like import ru.iris.common.* in java)
importPackage(Packages.ru.iris.common);
importPackage(Packages.ru.iris.common.helpers);

var action = advertisement.getResponse().getOutcome().getEntities().get("action").getValue();

if (action == "on") {
    new Speak().say("Включаю свет!");
}
else if (action == "off") {
    new Speak().say("Выключаю свет!");
}
else if (action == "dim") {
    new Speak().say("Приглушаю свет!");
}
else if (action == "bright") {
    new Speak().say("Делаю ярче свет!");
}
else {
    new Speak().say("Неизвестная команда!");
}
