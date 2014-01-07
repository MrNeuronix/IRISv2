package ru.iris.common.messaging.model.devices.zwave;

import com.google.gson.annotations.Expose;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class ZWaveDriverReady {

    @Expose
    private long homeId;

    /**
     * Default constructor for de-serialisation.
     */
    public ZWaveDriverReady set(long homeId) {
        this.homeId = homeId;
        return this;
    }

    public long getHomeId() {
        return homeId;
    }

    public void setHomeId(long homeId) {
        this.homeId = homeId;
    }

    @Override
    public String toString() {
        return "ZWaveDriverReady{ " + homeId + " }";
    }
}
