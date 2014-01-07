package ru.iris.common.messaging.model.devices.noolite;

import com.google.gson.annotations.Expose;
import ru.iris.common.devices.noolite.NooliteDevice;
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
public class ResponseNooliteDeviceInventoryAdvertisement extends Advertisement {
    /**
     * Device UUID
     */
    @Expose
    private NooliteDevice device;

    public ResponseNooliteDeviceInventoryAdvertisement set(NooliteDevice device) {
        this.device = device;
        return this;
    }

    public NooliteDevice getDevice() {
        return device;
    }

    public void setDevice(NooliteDevice device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return "ResponseNooliteDeviceInventoryAdvertisement{" +
                "device=" + device +
                '}';
    }
}
