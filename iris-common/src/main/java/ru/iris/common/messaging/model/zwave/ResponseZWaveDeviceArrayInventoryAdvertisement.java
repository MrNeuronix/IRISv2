package ru.iris.common.messaging.model.zwave;

import com.google.gson.annotations.Expose;
import ru.iris.common.devices.ZWaveDevice;
import ru.iris.common.messaging.model.Advertisement;

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
public class ResponseZWaveDeviceArrayInventoryAdvertisement extends Advertisement {
    /**
     * Devices Map
     */
    @Expose
    private Map<String, ZWaveDevice> devices;

    public ResponseZWaveDeviceArrayInventoryAdvertisement set(Map<String, ZWaveDevice> devices) {
        this.devices = devices;
        return this;
    }

    public Map<String, ZWaveDevice> getDevices() {
        return devices;
    }

    public void setDevices(Map<String, ZWaveDevice> devices) {
        this.devices = devices;
    }
}
