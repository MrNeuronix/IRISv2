package ru.iris.common.messaging.model.zwave;

import com.google.gson.JsonArray;
import com.google.gson.annotations.Expose;
import ru.iris.common.messaging.model.Advertisement;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class ResponseZWaveDeviceArrayInventoryAdvertisement extends Advertisement {
    /**
     * Devices Map
     */
    @Expose
    private JsonArray devices;

    public ResponseZWaveDeviceArrayInventoryAdvertisement set(JsonArray devices) {
        this.devices = devices;
        return this;
    }

    public JsonArray getDevices() {
        return devices;
    }

    public void setDevices(JsonArray devices) {
        this.devices = devices;
    }
}
