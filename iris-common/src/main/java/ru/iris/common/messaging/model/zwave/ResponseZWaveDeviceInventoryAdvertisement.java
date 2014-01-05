package ru.iris.common.messaging.model.zwave;

import com.google.gson.annotations.Expose;
import ru.iris.common.devices.zwave.ZWaveDevice;
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
public class ResponseZWaveDeviceInventoryAdvertisement extends Advertisement {
    /**
     * Device UUID
     */
    @Expose
    private ZWaveDevice device;

    public ResponseZWaveDeviceInventoryAdvertisement set(ZWaveDevice device) {
        this.device = device;
        return this;
    }

    public ZWaveDevice getDevice() {
        return device;
    }

    public void setDevice(ZWaveDevice device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return "ResponseZWaveDeviceInventoryAdvertisement {" +
                "zwaveDevice=" + device +
                '}';
    }
}
