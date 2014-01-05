package ru.iris.common.messaging.model;

import com.google.gson.annotations.Expose;

import java.util.Map;

/**
 * IRISv2 Project
 * Author: Nikolay A. Viguro
 * WWW: iris.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 19.11.13
 * Time: 11:34
 * License: GPL v3
 */
public class ResponseDeviceInventoryAdvertisement extends Advertisement {
    /**
     * Devices Map
     */
    @Expose
    private Map<String, Object> devices;

    public ResponseDeviceInventoryAdvertisement set(Map<String, Object> devices) {
        this.devices = devices;
        return this;
    }

    public Map<String, Object> getDevices() {
        return devices;
    }

    public void setDevices(Map<String, Object> devices) {
        this.devices = devices;
    }
}
