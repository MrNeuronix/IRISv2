package ru.iris.common.messaging.model;

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
public class SetDeviceNameAdvertisement extends Advertisement {
    /**
     * Device UUID
     */
    @Expose
    private String deviceUUID;
    /**
     * Device name.
     */
    @Expose
    private String name;

    public SetDeviceNameAdvertisement set(String deviceUUID, String name) {
        this.name = name;
        this.deviceUUID = deviceUUID;
        return this;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public void setDeviceUUID(String deviceUUID) {
        this.deviceUUID = deviceUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "SetDeviceNameAdvertisement { UUID: " + deviceUUID + ", name: " + name + " }";
    }
}
