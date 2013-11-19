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
public class GetInventoryAdvertisement extends Advertisement {
    /**
     * Device UUID
     */
    @Expose
    private String deviceUUID;

    public GetInventoryAdvertisement(String deviceUUID)
    {
        this.deviceUUID = deviceUUID;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public void setDeviceUUID(String deviceUUID) {
        this.deviceUUID = deviceUUID;
    }
}
