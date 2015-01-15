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

package ru.iris.common.modulestatus;

import com.avaje.ebean.Ebean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.iris.common.database.model.ModuleStatus;

import java.sql.Timestamp;
import java.util.Date;

public class Status {

    private static final Logger LOGGER = LogManager.getLogger(ModuleStatus.class);
    private String internalName;

    public Status(String name) {
        this.internalName = name;
    }

    public void running() {
        update("RUNNING");
    }

    public void stopped() {
        update("STOPPED");
    }

    public void crashed() {
        update("STOPPED");
    }

    private void update(String state) {
        ModuleStatus status = Ebean.find(ModuleStatus.class).where().eq("internalName", internalName).findUnique();

        if (status != null) {
            status.setLastseen(new Timestamp(new Date().getTime()));
            status.setStatus(state);
        } else {
            LOGGER.error("Error update module status! Module not found in DB!");
        }
    }

    public boolean checkExist() {
        ModuleStatus status = Ebean.find(ModuleStatus.class).where().eq("internalName", internalName).findUnique();
        return status != null;
    }

    public void addIntoDB(String name, String description) {
        ModuleStatus status = new ModuleStatus();
        status.setName(name);
        status.setInternalName(internalName);
        status.setDescription(description);
        status.setLastseen(new Timestamp(new Date().getTime()));
        status.setStatus("RUNNING");
        status.save();
    }
}
