package ru.iris.common.messaging.model.devices;

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
public class SetDeviceZoneAdvertisement extends Advertisement {
    /**
     * Device UUID
     */
    @Expose
    private String deviceUUID;
    /**
     * Device name.
     */
    @Expose
    private int zone;

    public SetDeviceZoneAdvertisement set(String deviceUUID, int zone) {
        this.zone = zone;
        this.deviceUUID = deviceUUID;
        return this;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public void setDeviceUUID(String deviceUUID) {
        this.deviceUUID = deviceUUID;
    }

    public int getZone() {
        return zone;
    }

    public void setZone(int zone) {
        this.zone = zone;
    }

    @Override
    public String toString() {
        return "SetDeviceZoneAdvertisement { UUID: " + deviceUUID + ", zone: " + zone + " }";
    }
}
