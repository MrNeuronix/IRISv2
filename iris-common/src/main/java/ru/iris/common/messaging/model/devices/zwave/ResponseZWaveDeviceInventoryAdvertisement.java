package ru.iris.common.messaging.model.devices.zwave;

import com.google.gson.annotations.Expose;
import ru.iris.common.database.model.devices.Device;
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
    private Device device;

    public ResponseZWaveDeviceInventoryAdvertisement set(Device device) {
        this.device = device;
        return this;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return "ResponseZWaveDeviceInventoryAdvertisement {" +
                "zwaveDevice=" + device +
                '}';
    }
}
