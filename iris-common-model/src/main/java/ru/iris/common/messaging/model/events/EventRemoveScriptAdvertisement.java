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

package ru.iris.common.messaging.model.events;

/**
 * Created by nikolay.viguro on 14.10.2014.
 */
public class EventRemoveScriptAdvertisement {
    private String name;
    private boolean isCommand = false;

    public EventRemoveScriptAdvertisement() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCommand() {
        return isCommand;
    }

    public void setCommand(boolean isCommand) {
        this.isCommand = isCommand;
    }

    @Override
    public String toString() {
        return "EventRemoveScriptAdvertisement{" +
                "name='" + name + '\'' +
                ", isCommand=" + isCommand +
                '}';
    }
}
