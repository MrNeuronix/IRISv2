package ru.iris.common.messaging.model;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

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
    private ArrayList devices;

    public ResponseZWaveDeviceArrayInventoryAdvertisement(ArrayList devices)
    {
        this.devices = devices;
    }

    public ArrayList getDevices() {
        return devices;
    }

    public void setDevices(ArrayList devices) {
        this.devices = devices;
    }
}
