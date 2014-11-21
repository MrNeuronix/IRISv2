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

package ru.iris.common.database.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "sensor_log")
public class SensorData extends DBModel {

    @Column(columnDefinition = "timestamp")
    private Timestamp logdate;

    private String uuid;
    private String sensor;
    private String value;

    public static void log(String uuid, String sensor, String value) {
        SensorData log = new SensorData();
        log.uuid = uuid;
        log.sensor = sensor;
        log.value = value;
        log.save();
    }

    // Default
    public SensorData() {
    }

    public Timestamp getLogdate() {
        return logdate;
    }

    public void setLogdate(Timestamp logdate) {
        this.logdate = logdate;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}